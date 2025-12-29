package noc.routing

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}

/** RingRouting - Ring routing algorithm Routes in clockwise or counter-clockwise
  *
  * @param config
  *   NoC configuration
  * @param numNodes
  *   Number of nodes
  */
class RingRouting(config: NoCConfig, numNodes: Int) extends DeterministicRouting(config) {
  override def getPossiblePorts(currentId: UInt, destId: UInt): chisel3.Vec[chisel3.Bool] = {
    val possiblePorts = Wire(Vec(config.totalPorts, Bool()))

    for (i <- 0 until config.totalPorts) { possiblePorts(i) := false.B }

    val current = currentId
    val dest = destId
    val diff = (dest - current + numNodes.U) % numNodes.U

    when(diff === 0.U) {
      // Reached destination
      possiblePorts(noc.config.Port.Local.id) := true.B
    }.elsewhen(diff <= (numNodes / 2).U) {
      // Clockwise direction
      possiblePorts(noc.config.Port.East.id) := true.B
    }.otherwise {
      // Counter-clockwise direction
      possiblePorts(noc.config.Port.West.id) := true.B
    }

    possiblePorts
  }
}

/**
  * RingRoutingGen - Generates Verilog for RingRouting Router
  */
// import noc.router.Router

// object RingRoutingGen extends App {
//   // ------------------------------------------------------------
//   // 1. Creating NoC configuration
//   // ------------------------------------------------------------
//   val config = NoCConfig(
//     dataWidth    = 32,
//     flitWidth    = 32,
//     vcNum        = 1,  // Single virtual channel
//     bufferDepth  = 4,  // Larger buffer for ring topology
//     nodeIdWidth  = 2,  // Support up to 4 nodes
//     numPorts     = 2,  // Ring: East + West
//     routingType  = "Ring",
//     topologyType = "Ring"
//   )

//   // ------------------------------------------------------------
//   // 2. Creating routing policy
//   // ------------------------------------------------------------
//   val numNodes = 4
//   val routingPolicy = new RingRouting(config, numNodes)

//   // ------------------------------------------------------------
//   // 3. Generating Verilog
//   // ------------------------------------------------------------
//   (new chisel3.stage.ChiselStage).emitVerilog(
//     new Router(config, routingPolicy),
//     Array(
//       "--target-dir", "rtl",
//       "--emission-options=disableMemRandomization,disableRegisterRandomization"
//     )
//   )
// }
