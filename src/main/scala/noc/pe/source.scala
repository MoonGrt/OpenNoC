package noc.pe

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * Source(NI)IO - Base IO interface for source components
 * Provides interface for sending flits to NoC
 *
 * @param config NoC configuration
 */
class SourceIO(val config: NoCConfig) extends Bundle {
  val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
  val flitOut = Decoupled(new Flit(config))
  val nodeId = Input(UInt(config.nodeIdWidth.W))  // This node's ID
}
class SourceStreamNIIO(val config: NoCConfig) extends Bundle {
  val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))  // Present but not used (ready = false)
  val streamOut = Decoupled(UInt(config.dataWidth.W))
  val nodeId = Input(UInt(config.nodeIdWidth.W))  // This node's ID
  val destId = Output(UInt(config.nodeIdWidth.W))
}

/**
 * Source(NI) - Abstract base class for source components
 * Sources generate and send data into the NoC network
 *
 * @param config NoC configuration
 */
abstract class Source(val config: NoCConfig) extends Module {
  // Subclasses define their own io bundles
}
abstract class SourceNI(val config: NoCConfig) extends Module {
  // Subclasses define their own io bundles
}

/**
 * FlitSource(NI) - Basic flit source
 * Takes flits from input and forwards them to NoC
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class FlitSource(config: NoCConfig) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

  // FlitSource doesn't generate flits by itself, it's a pass-through component
  // Since flitIn is unused, flitOut is always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare
}
class FlitSourceStreamNI(config: NoCConfig) extends SourceNI(config) {
  val io = IO(new Bundle {
    val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))  // Present but not used (ready = false)
    val streamOut = Decoupled(UInt(config.dataWidth.W))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Output(UInt(config.nodeIdWidth.W))
  })

  // streamIn is not used for source - always not ready
  io.streamIn.ready := false.B

  // FlitSource doesn't generate flits by itself, it's a pass-through component
  // Since streamIn is unused, flitOut is always invalid
  io.streamOut.valid := false.B
  io.streamOut.bits := DontCare
  io.destId := 0.U(config.nodeIdWidth.W)
}

/**
 * StreamSource(NI) - Stream data source
 * Takes data stream and packs it into flits for transmission
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class StreamSource(config: NoCConfig) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val dataIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val destId = Input(UInt(config.nodeIdWidth.W))  // Destination node ID
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

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
        io.flitOut.bits := Flit.head(config, io.nodeId, io.destId, sendQueue.io.deq.bits)
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
 * PacketSource(NI) - Packet source
 * Takes packets and sends them as flits
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param maxFlits Maximum number of flits per packet
 */
class PacketSource(config: NoCConfig, maxFlits: Int = 8) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val packetIn = Flipped(Decoupled(new Packet(config, maxFlits)))
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

  val packetReg = Reg(new Packet(config, maxFlits))
  val flitIndex = RegInit(0.U(log2Ceil(maxFlits).W))

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
 * CounterSource(NI) - Counter-based source
 * Generates a sequence of incrementing counter values as flits
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param countLimit Maximum count value
 */
class CounterSource(config: NoCConfig, countLimit: Int = 1000) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val count = Output(UInt(32.W))
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

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
      io.flitOut.bits := Flit.headTail(config, io.nodeId, io.destId, counter, 0.U)

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
 * BurstSource(NI) - Burst data source
 * Sends bursts of data packets
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 * @param burstSize Number of flits per burst
 */
class BurstSource(config: NoCConfig, burstSize: Int = 8) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val dataIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

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
        io.flitOut.bits := Flit.head(config, io.nodeId, io.destId, dataQueue.io.deq.bits)
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
 * PatternSource(NI) - Pattern-based source
 * Generates data according to a programmable pattern
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class PatternSource(config: NoCConfig) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val pattern = Input(UInt(config.dataWidth.W))  // Pattern value
    val patternEnable = Input(Bool())
    val start = Input(Bool())
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

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
      io.flitOut.bits := Flit.headTail(config, io.nodeId, sendDestId, io.pattern, 0.U)

      when(io.flitOut.ready) {
        sendState := 0.U
      }
    }
  }
}

/**
 * RandomSource(NI) - Random data source
 * Generates pseudo-random data values
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class RandomSource(config: NoCConfig) extends Source(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))  // Present but not used (ready = false)
    val flitOut = Decoupled(new Flit(config))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val enable = Input(Bool())
    val seed = Input(UInt(config.dataWidth.W))
  })

  // flitIn is not used for source - always not ready
  io.flitIn.ready := false.B

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
      io.flitOut.bits := Flit.headTail(config, io.nodeId, io.destId, lfsr, 0.U)

      when(io.flitOut.ready) {
        lfsr := lfsrNext(lfsr)
        sendState := 0.U
      }
    }
  }
}
class RandomSourceStreamNI(config: NoCConfig) extends SourceNI(config) {
  val io = IO(new Bundle {
    val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))  // Present but not used (ready = false)
    val streamOut = Decoupled(UInt(config.dataWidth.W))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Output(UInt(config.nodeIdWidth.W))
    val enable = Input(Bool())
    val seed = Input(UInt(config.dataWidth.W))
  })

  // streamIn is not used for source - always not ready
  io.streamIn.ready := false.B

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

  io.destId := DontCare
  io.streamOut.valid := false.B
  io.streamOut.bits := DontCare

  switch(sendState) {
    is(0.U) {
      when(io.enable) {
        sendState := 1.U
        lfsr := io.seed
      }
    }
    is(1.U) {
      io.destId := lfsr(config.nodeIdWidth - 1, 0)
      io.streamOut.valid := true.B
      io.streamOut.bits := lfsr

      when(io.streamOut.ready) {
        lfsr := lfsrNext(lfsr)
        sendState := 0.U
      }
    }
  }
}
