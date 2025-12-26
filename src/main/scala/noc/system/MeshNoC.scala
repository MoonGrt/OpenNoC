package noc.system

import chisel3._
import chisel3.util._
import chisel3.stage._
import noc.config.NoCConfig
import noc.router.{Router, RouterBuilder, RouterIO}
import noc.ni.{NetworkInterface, StreamNI}
import noc.topology.{NoCTopology, MeshTopology}

/**
 * MeshNoC - 2D Mesh NoC system
 *
 * @param config NoC configuration
 * @param width Mesh width
 * @param height Mesh height
 */
class MeshNoC(config: NoCConfig, val width: Int, val height: Int) extends NoC(config) {
  require(width > 0 && height > 0, "Mesh dimensions must be positive")

  val numNodes = width * height

  // Create topology
  val topology = new MeshTopology(config, width, height)

  // Create routers
  val routers = Seq.fill(numNodes) {
    RouterBuilder.buildXYRouter(config, width, height)
  }

  // Create network interfaces
  val networkInterfaces = (0 until numNodes).map { i =>
    Module(new StreamNI(config, i))
  }

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

  override def getNetworkInterfaces: Seq[NetworkInterface] = networkInterfaces

  override def getRouters: Seq[Router] = routers

  override def getTopology: NoCTopology = topology
}
/**
 * MeshNoCExample - Example of creating a 2D Mesh NoC
 *
 * This example demonstrates how to:
 * 1. Create a NoC configuration
 * 2. Instantiate a Mesh NoC system
 * 3. Access network interfaces for communication
 */
class MeshNoCExample extends Module {
  // Create NoC configuration
  val config = NoCConfig(
    dataWidth = 32,
    flitWidth = 32,
    vcNum = 2,           // 2 virtual channels
    bufferDepth = 4,      // Buffer depth of 4
    nodeIdWidth = 8,      // Support up to 256 nodes
    numPorts = 4,         // 4 ports (North, South, East, West) + Local
    routingType = "XY",
    topologyType = "Mesh"
  )

  // Create a 4x4 Mesh NoC (16 nodes)
  val meshNoC = Module(new MeshNoC(config, width = 4, height = 4))

  // Access network interfaces
  val nis = meshNoC.getNetworkInterfaces

  // Example: Connect to node 0's network interface
  val node0NI = nis(0)

  // Example: Connect to node 15's network interface (last node)
  val node15NI = nis(15)

  // You can now connect your processing elements to the network interfaces
  // For example:
  //   node0NI.io.streamIn <> yourPE0.io.dataOut
  //   yourPE0.io.dataIn <> node0NI.io.streamOut
  //   node0NI.io.destId := yourPE0.io.destId
}

// object MeshNoCExample extends App {
//   (new ChiselStage).emitVerilog(new MeshNoCExample, Array("--target-dir", "rtl"))
// }
