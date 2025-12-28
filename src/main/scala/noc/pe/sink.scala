package noc.pe

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * Sink(NI)IO - Base IO interface for sink components
 * Provides interface for receiving flits from NoC
 *
 * @param config NoC configuration
 */
class SinkIO(val config: NoCConfig) extends Bundle {
  val flitIn = Flipped(Decoupled(new Flit(config)))
  val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
  val nodeId = Input(UInt(config.nodeIdWidth.W))  // This node's ID
}
class SinkStreamNIIO(val config: NoCConfig) extends Bundle {
  val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
  val streamOut = Decoupled(UInt(config.dataWidth.W))  // Present but not used (valid = false)
  val nodeId = Input(UInt(config.nodeIdWidth.W))  // This no`de's ID
  val destId = Output(UInt(config.nodeIdWidth.W))  // Present but not used (valid = false)
}

/**
 * Sink(NI) - Abstract base class for sink components
 * Sinks receive and process data from the NoC network
 *
 * @param config NoC configuration
 */
abstract class Sink(val config: NoCConfig) extends Module {
  // Subclasses define their own io bundles
}
abstract class SinkNI(val config: NoCConfig) extends Module {
  // Subclasses define their own io bundles
}

/**
 * FlitSink(NI) - Basic flit sink
 * Receives flits and outputs them through a stream interface
 *
 * @param config NoC configuration
 */
class FlitSink(config: NoCConfig) extends Sink(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))
    val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  // Simple destroy flit input
  io.flitIn.ready := true.B
}
class FlitSinkStreamNI(config: NoCConfig) extends SinkNI(config) {
  val io = IO(new Bundle {
    val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val streamOut = Decoupled(UInt(config.dataWidth.W))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Output(UInt(config.nodeIdWidth.W))  // Present but not used (valid = false)
  })

  io.destId := io.nodeId

  // streamOut is not used for sink - always invalid
  io.streamOut.valid := false.B
  io.streamOut.bits := DontCare

  // Simple destroy stream input
  io.streamIn.ready := true.B
}

/**
 * StreamSink(NI) - Stream data sink
 * Receives flits from NoC, unpacks them, and outputs as data stream
 *
 * @param config NoC configuration
 */
class StreamSink(config: NoCConfig) extends Sink(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))
    val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val dataOut = Decoupled(UInt(config.dataWidth.W))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  val recvQueue = Module(new Queue(UInt(config.dataWidth.W), config.bufferDepth))
  val recvState = RegInit(0.U(2.W))  // 0: waiting head, 1: receiving

  io.flitIn.ready := false.B
  recvQueue.io.enq.valid := false.B
  recvQueue.io.enq.bits := DontCare

  switch(recvState) {
    is(0.U) {
      // Wait for head flit
      when(io.flitIn.valid && io.flitIn.bits.isHead) {
        recvQueue.io.enq.valid := true.B
        recvQueue.io.enq.bits := io.flitIn.bits.data
        io.flitIn.ready := recvQueue.io.enq.ready
        when(io.flitIn.bits.isTail) {
          // Single-flit packet
          recvState := 0.U
        }.otherwise {
          recvState := 1.U
        }
      }
    }
    is(1.U) {
      // Receive body flit or tail flit
      when(io.flitIn.valid) {
        recvQueue.io.enq.valid := true.B
        recvQueue.io.enq.bits := io.flitIn.bits.data
        io.flitIn.ready := recvQueue.io.enq.ready
        when(io.flitIn.bits.isTail) {
          recvState := 0.U
        }
      }
    }
  }

  io.dataOut <> recvQueue.io.deq
}

/**
 * PacketSink(NI) - Packet sink
 * Receives flits and reassembles them into packets
 *
 * @param config NoC configuration
 * @param maxFlits Maximum number of flits per packet
 */
class PacketSink(config: NoCConfig, maxFlits: Int = 8) extends Sink(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))
    val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val packetOut = Decoupled(new Packet(config, maxFlits))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  val packetBuffer = Reg(Vec(maxFlits, new Flit(config)))
  val packetLength = RegInit(0.U(log2Ceil(maxFlits + 1).W))
  val recvState = RegInit(0.U(2.W))  // 0: waiting head, 1: receiving
  val packetValid = RegInit(false.B)
  val packetReady = Wire(Bool())

  io.flitIn.ready := false.B
  io.packetOut.valid := false.B
  io.packetOut.bits := DontCare

  switch(recvState) {
    is(0.U) {
      // Wait for head flit
      when(io.flitIn.valid && io.flitIn.bits.isHead) {
        packetBuffer(0) := io.flitIn.bits
        packetLength := 1.U
        io.flitIn.ready := true.B

        when(io.flitIn.bits.isTail) {
          // Single-flit packet
          packetValid := true.B
          recvState := 0.U
        }.otherwise {
          recvState := 1.U
        }
      }
    }
    is(1.U) {
      // Receive body/tail flits
      when(io.flitIn.valid) {
        packetBuffer(packetLength) := io.flitIn.bits
        packetLength := packetLength + 1.U
        io.flitIn.ready := true.B

        when(io.flitIn.bits.isTail) {
          packetValid := true.B
          recvState := 0.U
        }
      }
    }
  }

  // Output assembled packet
  when(packetValid && packetReady) {
    packetValid := false.B
    packetLength := 0.U
  }

  packetReady := !packetValid || io.packetOut.ready
  io.packetOut.valid := packetValid

  for (i <- 0 until maxFlits) {
    if (i == 0) {
      io.packetOut.bits.flits(i) := packetBuffer(0)
    } else {
      io.packetOut.bits.flits(i) := Mux(i.U < packetLength, packetBuffer(i), Flit.empty(config))
    }
  }
  io.packetOut.bits.length := packetLength
  io.packetOut.bits.valid := true.B
}

/**
 * CounterSink(NI) - Counting sink
 * Counts the number of received flits and packets
 *
 * @param config NoC configuration
 */
class CounterSink(config: NoCConfig) extends Sink(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))
    val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val flitCount = Output(UInt(32.W))
    val packetCount = Output(UInt(32.W))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  val flitCounter = RegInit(0.U(32.W))
  val packetCounter = RegInit(0.U(32.W))
  val recvState = RegInit(0.U(2.W))  // Track if receiving a packet

  io.flitIn.ready := true.B

  when(io.flitIn.valid && io.flitIn.ready) {
    flitCounter := flitCounter + 1.U
    when(io.flitIn.bits.isHead) {
      when(io.flitIn.bits.isTail) {
        // Single-flit packet
        packetCounter := packetCounter + 1.U
      }.otherwise {
        recvState := 1.U
      }
    }.elsewhen(io.flitIn.bits.isTail && recvState === 1.U) {
      packetCounter := packetCounter + 1.U
      recvState := 0.U
    }
  }

  io.flitCount := flitCounter
  io.packetCount := packetCounter
}

/**
 * StatisticsSink(NI) - Statistics collecting sink
 * Collects various statistics about received data
 *
 * @param config NoC configuration
 */
class StatisticsSink(config: NoCConfig) extends Sink(config) {
  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(config)))
    val flitOut = Decoupled(new Flit(config))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val flitCount = Output(UInt(32.W))
    val packetCount = Output(UInt(32.W))
    val headFlitCount = Output(UInt(32.W))
    val bodyFlitCount = Output(UInt(32.W))
    val tailFlitCount = Output(UInt(32.W))
    val bytesReceived = Output(UInt(64.W))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  val flitCounter = RegInit(0.U(32.W))
  val packetCounter = RegInit(0.U(32.W))
  val headFlitCounter = RegInit(0.U(32.W))
  val bodyFlitCounter = RegInit(0.U(32.W))
  val tailFlitCounter = RegInit(0.U(32.W))
  val bytesCounter = RegInit(0.U(64.W))
  val recvState = RegInit(0.U(2.W))

  io.flitIn.ready := true.B

  when(io.flitIn.valid && io.flitIn.ready) {
    flitCounter := flitCounter + 1.U
    bytesCounter := bytesCounter + (config.flitWidth / 8).U

    when(io.flitIn.bits.isHead) {
      headFlitCounter := headFlitCounter + 1.U
      when(io.flitIn.bits.isTail) {
        packetCounter := packetCounter + 1.U
      }.otherwise {
        recvState := 1.U
      }
    }.elsewhen(io.flitIn.bits.isBody) {
      bodyFlitCounter := bodyFlitCounter + 1.U
    }.elsewhen(io.flitIn.bits.isTail) {
      tailFlitCounter := tailFlitCounter + 1.U
      when(recvState === 1.U) {
        packetCounter := packetCounter + 1.U
        recvState := 0.U
      }
    }
  }

  io.flitCount := flitCounter
  io.packetCount := packetCounter
  io.headFlitCount := headFlitCounter
  io.bodyFlitCount := bodyFlitCounter
  io.tailFlitCount := tailFlitCounter
  io.bytesReceived := bytesCounter
}
