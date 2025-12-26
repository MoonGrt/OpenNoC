package noc.system

import chisel3._
import chisel3.util._
import chisel3.stage._
import noc.config.NoCConfig
import noc.router.{Router, RouterBuilder, RouterIO}
import noc.ni.{NetworkInterface, StreamNI}
import noc.topology.{NoCTopology, RingTopology}
import noc.routing.RoutingPolicy
import noc.routing.DeterministicRouting

/**
 * RingNoC - Ring NoC system
 *
 * @param config NoC configuration
 * @param numNodes Number of nodes
 */
class RingNoC(config: NoCConfig, val numNodes: Int) extends NoC(config) {
  require(numNodes > 0, "Number of nodes must be positive")

  // Create topology
  val topology = new RingTopology(config, numNodes)

  // Create simple routing policy (ring routing)
  class RingRouting(config: NoCConfig, numNodes: Int) extends DeterministicRouting(config) {
    override def getPossiblePorts(currentId: UInt, destId: UInt): chisel3.Vec[chisel3.Bool] = {
      val possiblePorts = Wire(Vec(config.totalPorts, Bool()))

      for (i <- 0 until config.totalPorts) {
        possiblePorts(i) := false.B
      }

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

  // Create routers
  val routingPolicy: RoutingPolicy = new RingRouting(config, numNodes)
  val routers = Seq.fill(numNodes) {
    Module(new Router(config, routingPolicy))
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
 * RingNoCExample - Example of creating a Ring NoC
 *
 * This example demonstrates how to:
 * 1. Create a NoC configuration for ring topology
 * 2. Instantiate a Ring NoC system
 * 3. Use the network interfaces for communication
 */
class RingNoCExample extends Module {
  // Create NoC configuration for ring topology
  val config = NoCConfig(
    dataWidth = 32,
    flitWidth = 32,
    vcNum = 1,           // Single virtual channel
    bufferDepth = 8,     // Larger buffer for ring topology
    nodeIdWidth = 6,     // Support up to 64 nodes
    numPorts = 2,        // 2 ports (East, West) + Local
    routingType = "Ring",
    topologyType = "Ring"
  )

  // Create a Ring NoC with 8 nodes
  val ringNoC = Module(new RingNoC(config, numNodes = 8))

  // Access network interfaces
  val nis = ringNoC.getNetworkInterfaces

  // Example: Access all network interfaces
  for (i <- 0 until 8) {
    val ni = nis(i)
    // Connect your processing elements here
  }
}

object RingNoCExample extends App {
  (new ChiselStage).emitVerilog(new RingNoCExample, Array("--target-dir", "rtl"))
}
