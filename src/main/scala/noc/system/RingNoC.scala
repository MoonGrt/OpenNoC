package noc.system

import chisel3._
import chisel3.util._
import noc.config.NoCConfig
import noc.router.{Router, RouterBuilder, RouterIO}
import noc.ni.{NetworkInterface, StreamNI}
import noc.topology.{NoCTopology, RingTopology}
import noc.routing.{RoutingPolicy, RingRouting}

/**
 * RingNoC - Ring NoC system
 *
 * @param config NoC configuration
 * @param numNodes Number of nodes
 */
class RingNoC(config: NoCConfig, val numNodes: Int) extends NoC(config) {
  require(numNodes > 0, "Number of nodes must be positive")

  val io = IO(new Bundle {
    val nodeId = Output(Vec(numNodes, UInt(config.nodeIdWidth.W)))
    val destId = Input(Vec(numNodes, UInt(config.nodeIdWidth.W)))
    val streamIn  = Flipped(Vec(numNodes, Decoupled(UInt(config.flitWidth.W))))
    val streamOut = Vec(numNodes, Decoupled(UInt(config.flitWidth.W)))
  })

  // Create topology
  val topology = new RingTopology(config, numNodes)

  // Create routers
  val routingPolicy: RoutingPolicy = new RingRouting(config, numNodes)
  val routers = Seq.fill(numNodes) { Module(new Router(config, routingPolicy)) }
  // Create network interfaces
  val networkInterfaces = (0 until numNodes).map { i => Module(new StreamNI(config, i)) }

  // Connect network interfaces to router Local ports
  for (i <- 0 until numNodes) {
    routers(i).io.routerId := i.U
    routers(i).io.congestionInfo := VecInit(Seq.fill(config.totalPorts)(0.U(8.W)))
    // NI connected to router Local port
    routers(i).io.inPorts(noc.config.Port.Local.id) <> networkInterfaces(i).io.routerLink.out
    networkInterfaces(i).io.routerLink.in <> routers(i).io.outPorts(noc.config.Port.Local.id)
  }

  // Connect routers (through topology)
  topology.connectRouters(routers.map(_.io))

  for (i <- 0 until numNodes) {
    io.nodeId(i)    <> networkInterfaces(i).io.nodeId
    io.destId(i)    <> networkInterfaces(i).io.destId
    io.streamIn(i)  <> networkInterfaces(i).io.streamIn
    io.streamOut(i) <> networkInterfaces(i).io.streamOut
  }

  override def getNetworkInterfaces: Seq[StreamNI] = networkInterfaces  // StreamNI instead of NetworkInterface

  override def getRouters: Seq[Router] = routers

  override def getTopology: NoCTopology = topology
}

object RingNoC extends App {
  val config = NoCConfig(
    dataWidth    = 32,
    flitWidth    = 32,
    vcNum        = 2,  // 2 virtual channels
    bufferDepth  = 4,  // Larger buffer for ring topology
    nodeIdWidth  = 2,  // Support up to 4 nodes
    numPorts     = 2,  // Ring: East + West
    routingType  = "Ring",
    topologyType = "Ring"
  )

  (new chisel3.stage.ChiselStage).emitVerilog(
    new RingNoC(config, numNodes = 4),
    Array(
      "--target-dir", "rtl",
      "--emission-options=disableMemRandomization,disableRegisterRandomization"
    )
  )
}

/**
 * RingNoCGen - Ring NoC system generator``
 *
 * This example demonstrates how to:
 * 1. Create a NoC configuration for ring topology
 * 2. Instantiate a Ring NoC system
 * 3. Use the network interfaces for communication
 */
// import noc.pe.{RandomSourceStreamNI, FlitSinkStreamNI}

// class RingNoCGen extends Module {
//   val io = IO(new Bundle {
//     val enable = Input(Bool())
//   })

//   // Create NoC configuration for ring topology
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

//   // Create a Ring NoC with 4 nodes
//   val ringNoC = Module(new RingNoC(config, numNodes = 4))

//   // Example
//   // Connect processing elements to network interfaces
//   for (i <- 0 until 2) {
//     // Create processing elements
//     val source = Module(new RandomSourceStreamNI(config))
//     val sink = Module(new FlitSinkStreamNI(config))

//     // Connect processing elements to network interfaces
//     source.io.enable := io.enable
//     source.io.seed := 7.U(config.dataWidth.W)
//     source.io.nodeId <> ringNoC.io.nodeId(i)
//     source.io.destId <> ringNoC.io.destId(i)
//     source.io.streamOut <> ringNoC.io.streamIn(i)
//     source.io.streamIn <> ringNoC.io.streamOut(i)

//     sink.io.nodeId <> ringNoC.io.nodeId(i+2)
//     sink.io.destId <> ringNoC.io.destId(i+2)
//     sink.io.streamIn <> ringNoC.io.streamOut(i+2)
//     sink.io.streamOut <> ringNoC.io.streamIn(i+2)
//   }
// }

// object RingNoCGen extends App {
//   (new chisel3.stage.ChiselStage).emitVerilog(new RingNoCGen, Array("--target-dir", "rtl"))
// }
