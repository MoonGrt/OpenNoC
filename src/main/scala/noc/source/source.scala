package noc.source

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * SourceIO - Base IO interface for source components
 * Provides interface for sending flits to NoC
 *
 * @param config NoC configuration
 */
class SourceIO(val config: NoCConfig) extends Bundle {
  val flitOut = Decoupled(new Flit(config))
  val nodeId = Output(UInt(config.nodeIdWidth.W))  // This node's ID
}

/**
 * Source - Abstract base class for source components
 * Sources generate and send data into the NoC network
 *
 * @param config NoC configuration
 */
abstract class Source(val config: NoCConfig, val nodeId: Int) extends Module {
  // Subclasses define their own io bundles
}

/**
 * FlitSource - Basic flit source
 * Takes flits from input and forwards them to NoC
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class FlitSource(config: NoCConfig, nodeId: Int) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val flitIn = Flipped(Decoupled(new Flit(config)))
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  io.flitOut <> io.flitIn
}

/**
 * StreamSource - Stream data source
 * Takes data stream and packs it into flits for transmission
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class StreamSource(config: NoCConfig, nodeId: Int) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val dataIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val destId = Input(UInt(config.nodeIdWidth.W))  // Destination node ID
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val sendQueue = Module(new Queue(UInt(config.dataWidth.W), config.bufferDepth))
  sendQueue.io.enq <> io.dataIn
  
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending head, 2: sending body/tail
  val sendCounter = RegInit(0.U(8.W))
  val sendDestId = Reg(UInt(config.nodeIdWidth.W))
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  sendQueue.io.deq.ready := false.B
  
  switch(sendState) {
    is(0.U) {
      // Wait for data
      when(sendQueue.io.deq.valid) {
        sendState := 1.U
        sendDestId := io.destId
        sendCounter := 1.U
        // Send head flit
        io.flitOut.valid := true.B
        io.flitOut.bits := Flit.head(config, nodeId.U, io.destId, sendQueue.io.deq.bits)
        sendQueue.io.deq.ready := io.flitOut.ready
      }
    }
    is(1.U) {
      // Send body flit or tail flit
      when(sendQueue.io.deq.valid) {
        // Check if there's more data in queue (simplified: use counter to limit packet size)
        val isLast = sendCounter >= (config.bufferDepth - 1).U
        io.flitOut.valid := true.B
        when(isLast) {
          io.flitOut.bits := Flit.tail(config, sendQueue.io.deq.bits)
          sendState := 0.U
          sendCounter := 0.U
        }.otherwise {
          io.flitOut.bits := Flit.body(config, sendQueue.io.deq.bits)
          sendCounter := sendCounter + 1.U
        }
        sendQueue.io.deq.ready := io.flitOut.ready
      }
    }
  }
}

/**
 * PacketSource - Packet source
 * Takes packets and sends them as flits
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param maxFlits Maximum number of flits per packet
 */
class PacketSource(config: NoCConfig, nodeId: Int, maxFlits: Int = 8) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val packetIn = Flipped(Decoupled(new Packet(config, maxFlits)))
  })
  
  val packetReg = Reg(new Packet(config, maxFlits))
  val flitIndex = RegInit(0.U(log2Ceil(maxFlits).W))
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending
  val packetValid = RegInit(false.B)
  
  io.packetIn.ready := false.B
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  
  switch(sendState) {
    is(0.U) {
      // Wait for packet
      when(io.packetIn.valid) {
        packetReg := io.packetIn.bits
        packetValid := true.B
        flitIndex := 0.U
        sendState := 1.U
        io.packetIn.ready := true.B
      }
    }
    is(1.U) {
      // Send flits
      when(packetValid && flitIndex < packetReg.length) {
        io.flitOut.valid := true.B
        io.flitOut.bits := packetReg.flits(flitIndex)
        
        when(io.flitOut.ready) {
          val nextIndex = flitIndex + 1.U
          flitIndex := nextIndex
          when(nextIndex >= packetReg.length) {
            sendState := 0.U
            packetValid := false.B
          }
        }
      }
    }
  }
}

/**
 * CounterSource - Counter-based source
 * Generates a sequence of incrementing counter values as flits
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param countLimit Maximum count value
 */
class CounterSource(config: NoCConfig, nodeId: Int, countLimit: Int = 1000) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val count = Output(UInt(32.W))
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val counter = RegInit(0.U(32.W))
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  io.count := counter
  
  switch(sendState) {
    is(0.U) {
      when(io.start && counter < countLimit.U) {
        sendState := 1.U
      }
    }
    is(1.U) {
      io.flitOut.valid := true.B
      io.flitOut.bits := Flit.headTail(config, nodeId.U, io.destId, counter, 0.U)
      
      when(io.flitOut.ready) {
        counter := counter + 1.U
        when(counter >= (countLimit - 1).U) {
          sendState := 0.U
        }
      }
    }
  }
}

/**
 * BurstSource - Burst data source
 * Sends bursts of data packets
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param burstSize Number of flits per burst
 */
class BurstSource(config: NoCConfig, nodeId: Int, burstSize: Int = 8) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val dataIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending head, 2: sending body/tail
  val flitCount = RegInit(0.U(log2Ceil(burstSize + 1).W))
  val sendDestId = Reg(UInt(config.nodeIdWidth.W))
  val dataReg = Reg(UInt(config.dataWidth.W))
  
  val dataQueue = Module(new Queue(UInt(config.dataWidth.W), config.bufferDepth))
  dataQueue.io.enq <> io.dataIn
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  dataQueue.io.deq.ready := false.B
  
  switch(sendState) {
    is(0.U) {
      when(io.start && dataQueue.io.deq.valid) {
        sendState := 1.U
        sendDestId := io.destId
        flitCount := 1.U
        dataReg := dataQueue.io.deq.bits
        // Send head flit
        io.flitOut.valid := true.B
        io.flitOut.bits := Flit.head(config, nodeId.U, io.destId, dataQueue.io.deq.bits)
        dataQueue.io.deq.ready := io.flitOut.ready
      }
    }
    is(1.U) {
      val isLast = flitCount === (burstSize - 1).U
      when(dataQueue.io.deq.valid) {
        io.flitOut.valid := true.B
        when(isLast) {
          io.flitOut.bits := Flit.tail(config, dataQueue.io.deq.bits)
          sendState := 0.U
          flitCount := 0.U
        }.otherwise {
          io.flitOut.bits := Flit.body(config, dataQueue.io.deq.bits)
          flitCount := flitCount + 1.U
        }
        dataQueue.io.deq.ready := io.flitOut.ready
      }
    }
  }
}

/**
 * PatternSource - Pattern-based source
 * Generates data according to a programmable pattern
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class PatternSource(config: NoCConfig, nodeId: Int) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val pattern = Input(UInt(config.dataWidth.W))  // Pattern value
    val patternEnable = Input(Bool())
    val start = Input(Bool())
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val sendState = RegInit(0.U(2.W))
  val sendDestId = Reg(UInt(config.nodeIdWidth.W))
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  
  switch(sendState) {
    is(0.U) {
      when(io.start && io.patternEnable) {
        sendState := 1.U
        sendDestId := io.destId
      }
    }
    is(1.U) {
      io.flitOut.valid := true.B
      io.flitOut.bits := Flit.headTail(config, nodeId.U, sendDestId, io.pattern, 0.U)
      
      when(io.flitOut.ready) {
        sendState := 0.U
      }
    }
  }
}

/**
 * MemorySource - Memory-backed source
 * Reads data from memory and sends it as flits
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param bufferDepth Depth of the memory buffer
 */
class MemorySource(config: NoCConfig, nodeId: Int, bufferDepth: Int = 256) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val dataIn = Input(Vec(bufferDepth, UInt(config.dataWidth.W)))
    val readAddr = Output(UInt(log2Ceil(bufferDepth).W))
    val length = Input(UInt(log2Ceil(bufferDepth + 1).W))  // Number of words to send
  })
  
  val memory = SyncReadMem(bufferDepth, UInt(config.dataWidth.W))
  val readPtr = RegInit(0.U(log2Ceil(bufferDepth).W))
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending head, 2: sending body/tail
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val sendDestId = Reg(UInt(config.nodeIdWidth.W))
  val sendLength = Reg(UInt(log2Ceil(bufferDepth + 1).W))
  val flitCount = RegInit(0.U(log2Ceil(bufferDepth + 1).W))
  val memWritePtr = RegInit(0.U(log2Ceil(bufferDepth).W))
  val memWriteEnable = RegInit(false.B)
  
  // Initialize memory from dataIn - write during initialization phase
  // This happens when start is first asserted
  val startEdge = io.start && !RegNext(io.start, false.B)
  
  when(startEdge && !memWriteEnable) {
    memWriteEnable := true.B
    memWritePtr := 0.U
  }
  
  when(memWriteEnable && memWritePtr < bufferDepth.U) {
    memory.write(memWritePtr, io.dataIn(memWritePtr))
    memWritePtr := memWritePtr + 1.U
    when(memWritePtr === (bufferDepth - 1).U) {
      memWriteEnable := false.B
    }
  }
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  io.readAddr := readPtr
  
  switch(sendState) {
    is(0.U) {
      // Wait for start signal and ensure memory is initialized
      when(io.start && !memWriteEnable) {
        sendState := 1.U
        sendDestId := io.destId
        sendLength := io.length
        readPtr := 0.U
        flitCount := 0.U
      }
    }
    is(1.U) {
      // Send head flit
      val data = memory.read(readPtr)
      io.flitOut.valid := true.B
      when(sendLength === 1.U) {
        io.flitOut.bits := Flit.headTail(config, nodeId.U, sendDestId, data, 0.U)
      }.otherwise {
        io.flitOut.bits := Flit.head(config, nodeId.U, sendDestId, data, 0.U)
      }
      
      when(io.flitOut.ready) {
        readPtr := readPtr + 1.U
        flitCount := flitCount + 1.U
        when(sendLength === 1.U) {
          sendState := 0.U
        }.otherwise {
          sendState := 2.U
        }
      }
    }
    is(2.U) {
      // Send body/tail flits
      val data = memory.read(readPtr)
      val isLast = flitCount === (sendLength - 1.U)
      
      io.flitOut.valid := true.B
      when(isLast) {
        io.flitOut.bits := Flit.tail(config, data, 0.U)
      }.otherwise {
        io.flitOut.bits := Flit.body(config, data, 0.U)
      }
      
      when(io.flitOut.ready) {
        readPtr := readPtr + 1.U
        flitCount := flitCount + 1.U
        when(isLast) {
          sendState := 0.U
        }
      }
    }
  }
}

/**
 * RandomSource - Random data source
 * Generates pseudo-random data values
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class RandomSource(config: NoCConfig, nodeId: Int) extends Source(config, nodeId) {
  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Output(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val enable = Input(Bool())
    val seed = Input(UInt(config.dataWidth.W))
  })
  
  io.nodeId := nodeId.U(config.nodeIdWidth.W)
  
  val lfsr = RegInit(0.U(config.dataWidth.W))  // Linear feedback shift register for pseudo-random
  val sendState = RegInit(0.U(2.W))
  
  // Initialize LFSR with seed when enable is first asserted
  when(io.enable && sendState === 0.U) {
    lfsr := io.seed
  }
  
  // Simple LFSR polynomial: x^32 + x^22 + x^2 + x + 1 (if dataWidth >= 32)
  def lfsrNext(value: UInt): UInt = {
    val width = config.dataWidth
    if (width <= 16) {
      // Smaller LFSR: x^16 + x^14 + x^13 + x^11 + 1
      Cat(value(width - 2, 0), value(15) ^ value(13) ^ value(12) ^ value(10))
    } else {
      // Larger LFSR
      val taps = width match {
        case w if w >= 32 => Seq(31, 21, 1, 0)
        case w if w >= 16 => Seq(15, 13, 12, 10)
        case _ => Seq(7, 6)
      }
      val feedback = taps.map(value(_)).reduce(_ ^ _)
      Cat(value(width - 2, 0), feedback)
    }
  }
  
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
  
  switch(sendState) {
    is(0.U) {
      when(io.enable) {
        sendState := 1.U
        lfsr := io.seed
      }
    }
    is(1.U) {
      io.flitOut.valid := true.B
      io.flitOut.bits := Flit.headTail(config, nodeId.U, io.destId, lfsr, 0.U)
      
      when(io.flitOut.ready) {
        lfsr := lfsrNext(lfsr)
        sendState := 0.U
      }
    }
  }
}
