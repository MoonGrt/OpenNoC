package demo

import chisel3._
import chisel3.util._
import noc.config.NoCConfig
import noc.system.MeshNoC
import noc.ni.StreamNI

/**
 * CompleteExample - Complete example of a NoC system with processing elements
 *
 * This example demonstrates a complete system with:
 * 1. A Mesh NoC
 * 2. Simple processing elements connected to network interfaces
 * 3. Data transmission between nodes
 */
class SimplePE(val config: NoCConfig, val nodeId: Int) extends Module {
  val io = IO(new Bundle {
    val dataOut = Decoupled(UInt(config.dataWidth.W))
    val dataIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val destId = Output(UInt(config.nodeIdWidth.W))
    val start = Input(Bool())
  })

  val counter = RegInit(0.U(32.W))
  val state = RegInit(0.U(2.W))

  io.dataOut.valid := false.B
  io.dataOut.bits := DontCare
  io.destId := (nodeId + 1).U % 16.U  // Send to next node

  switch(state) {
    is(0.U) {
      when(io.start) {
        state := 1.U
        counter := 0.U
      }
    }
    is(1.U) {
      io.dataOut.valid := true.B
      io.dataOut.bits := counter
      when(io.dataOut.ready) {
        counter := counter + 1.U
        when(counter === 10.U) {
          state := 0.U
        }
      }
    }
  }

  io.dataIn.ready := true.B
}

class CompleteExample extends Module {
  val config = NoCConfig(
    dataWidth = 32,
    vcNum = 2,
    bufferDepth = 4,
    nodeIdWidth = 8,
    numPorts = 4,
    routingType = "XY",
    topologyType = "Mesh"
  )

  // Create a 4x4 Mesh NoC
  val meshNoC = Module(new MeshNoC(config, width = 4, height = 4))

  // Create processing elements
  val pes = (0 until 16).map { i =>
    Module(new SimplePE(config, i))
  }

  // Connect PEs to network interfaces
  for (i <- 0 until 16) {
    val ni = meshNoC.getNetworkInterfaces(i)

    // Connect PE output to NI input
    ni.io.streamIn <> pes(i).io.dataOut
    ni.io.destId := pes(i).io.destId

    // Connect NI output to PE input
    pes(i).io.dataIn <> ni.io.streamOut
  }

  // Example: Start node 0
  pes(0).io.start := true.B
  for (i <- 1 until 16) {
    pes(i).io.start := false.B
  }
}

// object CompleteExample extends App {
//   (new chisel3.stage.ChiselStage).emitVerilog(new CompleteExample, Array("--target-dir", "rtl"))
// }



