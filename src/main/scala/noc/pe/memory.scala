package noc.pe

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * MemorySink - Memory-backed sink
 * Stores received data in a memory buffer
 *
 * @param config NoC configuration
 * @param bufferDepth Depth of the memory buffer
 */
class MemorySink(config: NoCConfig, bufferDepth: Int = 256) extends Sink(config) {
  import config._

  val io = IO(new Bundle {
    val flitIn = Flipped(Decoupled(new Flit(flitConfig)))
    val flitOut = Decoupled(new Flit(flitConfig))  // Present but not used (valid = false)
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val dataOut = Output(Vec(bufferDepth, UInt(config.dataWidth.W)))
    val writeAddr = Output(UInt(log2Ceil(bufferDepth).W))
    val writeEn = Output(Bool())
    val readAddr = Input(UInt(log2Ceil(bufferDepth).W))
    val readData = Output(UInt(config.dataWidth.W))
  })

  // flitOut is not used for sink - always invalid
  io.flitOut.valid := false.B
  io.flitOut.bits := DontCare

  val memory = SyncReadMem(bufferDepth, UInt(config.dataWidth.W))
  val writePtr = RegInit(0.U(log2Ceil(bufferDepth).W))
  val recvState = RegInit(0.U(2.W))

  io.flitIn.ready := writePtr < (bufferDepth - 1).U  // Not full

  when(io.flitIn.valid && io.flitIn.ready) {
    when(io.flitIn.bits.isHead) {
      memory.write(writePtr, io.flitIn.bits.data)
      writePtr := writePtr + 1.U
      when(io.flitIn.bits.isTail) {
        // Single-flit packet done
      }.otherwise {
        recvState := 1.U
      }
    }.elsewhen(recvState === 1.U) {
      memory.write(writePtr, io.flitIn.bits.data)
      writePtr := writePtr + 1.U
      when(io.flitIn.bits.isTail) {
        recvState := 0.U
      }
    }
  }

  // Read port
  io.readData := memory.read(io.readAddr)

  // Output current state
  io.writeAddr := writePtr
  io.writeEn := io.flitIn.valid && io.flitIn.ready

  // Output memory contents (for debugging/monitoring)
  for (i <- 0 until bufferDepth) {
    io.dataOut(i) := memory.read(i.U)
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
class MemorySource(config: NoCConfig, bufferDepth: Int = 256) extends Source(config) {
  import config._

  val io = IO(new Bundle {
    val flitOut = Decoupled(new Flit(flitConfig))
    val nodeId = Input(UInt(config.nodeIdWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
    val dataIn = Input(Vec(bufferDepth, UInt(config.dataWidth.W)))
    val readAddr = Output(UInt(log2Ceil(bufferDepth).W))
    val length = Input(UInt(log2Ceil(bufferDepth + 1).W))  // Number of words to send
  })

  val memory = SyncReadMem(bufferDepth, UInt(config.dataWidth.W))
  val readPtr = RegInit(0.U(log2Ceil(bufferDepth).W))
  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending head, 2: sending body/tail

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
        io.flitOut.bits := Flit.headTail(flitConfig, vcId = 0.U, srcId = 0.U, sendDestId, data)
      }.otherwise {
        io.flitOut.bits := Flit.head(flitConfig, vcId = 0.U, srcId = 0.U, sendDestId, data)
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
        io.flitOut.bits := Flit.tail(flitConfig, data, 0.U)
      }.otherwise {
        io.flitOut.bits := Flit.body(flitConfig, data, 0.U)
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
