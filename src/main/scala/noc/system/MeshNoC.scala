package noc.system

import chisel3._
import chisel3.util._
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

  val io = IO(new Bundle {
    val nodeId = Output(Vec(numNodes, UInt(config.nodeIdWidth.W)))
    val destId = Input(Vec(numNodes, UInt(config.nodeIdWidth.W)))
    val streamIn  = Flipped(Vec(numNodes, Decoupled(UInt(config.flitWidth.W))))
    val streamOut = Vec(numNodes, Decoupled(UInt(config.flitWidth.W)))
  })

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


/**
 * MeshNoCGen - Generates a 2D Mesh NoC system
 *
 * This example demonstrates how to:
 * 1. Create a NoC configuration
 * 2. Instantiate a Mesh NoC system
 * 3. Access network interfaces for communication
 */
import noc.pe.{RandomSourceStreamNI, FlitSinkStreamNI}

class MeshNoCGen extends Module {
  // Create NoC configuration
  val config = NoCConfig(
    dataWidth    = 32,
    flitWidth    = 32,
    vcNum        = 1,  // 1 virtual channels
    bufferDepth  = 4,  // Larger buffer for ring topology
    nodeIdWidth  = 2,  // Support up to 4 nodes
    numPorts     = 4,  // 4 ports (North, South, East, West) + Local
    routingType  = "XY",
    topologyType = "Mesh"
  )

  // Create a 2x2 Mesh NoC (4 nodes)
  val meshNoC = Module(new MeshNoC(config, width = 2, height = 2))

  // Example
  // Connect processing elements to network interfaces
  for (i <- 0 until 2) {
    // Create processing elements
    val source = Module(new RandomSourceStreamNI(config))
    val sink = Module(new FlitSinkStreamNI(config))

    // Connect processing elements to network interfaces
    source.io.enable := true.B
    source.io.seed := 7.U(config.dataWidth.W)
    source.io.nodeId <> meshNoC.io.nodeId(i)
    source.io.destId <> meshNoC.io.destId(i)
    source.io.streamOut <> meshNoC.io.streamIn(i)
    source.io.streamIn <> meshNoC.io.streamOut(i)

    sink.io.nodeId <> meshNoC.io.nodeId(i+2)
    sink.io.destId <> meshNoC.io.destId(i+2)
    sink.io.streamIn <> meshNoC.io.streamOut(i+2)
    sink.io.streamOut <> meshNoC.io.streamIn(i+2)
  }
}

object MeshNoCGen extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MeshNoCGen, Array("--target-dir", "rtl"))
}
