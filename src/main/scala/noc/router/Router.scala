package noc.router

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig
import noc.switch.{SwitchFabric, Crossbar}
import noc.channel.UniBufferedChannel
import noc.routing.{RoutingPolicy, XYRouting, AdaptiveXYRouting}

/**
 * RouterIO - Router IO interface
 * Defines the input and output ports of a router
 *
 * @param config NoC configuration
 */
class RouterIO(val config: NoCConfig) extends Bundle {
  // Input ports (including Local port)
  val inPorts = Flipped(Vec(config.portNum, Decoupled(new Flit(config.flitConfig))))

  // Output ports (including Local port)
  val outPorts = Vec(config.portNum, Decoupled(new Flit(config.flitConfig)))

  // Router ID
  val routerId = Input(UInt(config.nodeIdWidth.W))

  // Optional credits for buffering
  // val incredits = Input(Vec(config.portNum, Bool()))
  // val increditsvcid = Input(Vec(config.portNum, UInt(config.vcIdWidth.W)))
  // val outcredits = Output(Vec(config.portNum, Bool()))
  // val outcreditsvcid = Output(Vec(config.portNum, UInt(config.vcIdWidth.W)))

  // Optional congestion information (for adaptive routing)
  // val congestionInfo = Input(Vec(config.portNum, UInt(8.W)))  // Congestion level for each port
}

/**
 * Router - Core component in NoC, responsible for routing and forwarding flits
 *
 * @param config NoC configuration
 * @param routingPolicy Routing policy
 */
class Router(val config: NoCConfig, val routingPolicy: RoutingPolicy) extends Module {
  val io = IO(new RouterIO(config))

  // Input buffers: create buffers for each input port and each VC
  val inputBuffers = Seq.fill(config.portNum) {
    Module(new VirtualChannel(config))
  }
  // Connect input ports to buffers
  for (port <- 0 until config.portNum) {
    inputBuffers(port).io.in <> io.inPorts(port)
  }

  // Route computation: compute output port for each input port
  val routeDecisions = Wire(Vec(config.portNum, UInt(config.portWidth.W)))
  val routeValids = Wire(Vec(config.portNum, Bool()))

  for (port <- 0 until config.portNum) {
    val flit = inputBuffers(port).io.out.bits
    val hasFlit = inputBuffers(port).io.out.valid

    // Compute routing decision (only compute for head flit, body and tail flits use previous routing decision)
    val routeDecision = routingPolicy.route(io.routerId, flit.dstId)
    routeDecisions(port) := routeDecision
    routeValids(port) := hasFlit && flit.isHead
  }

  // Switch fabric: implemented using Crossbar
  val crossbar = Module(new Crossbar(config, config.portNum, config.portNum))

  // Connect buffer outputs to Crossbar inputs
  for (port <- 0 until config.portNum) {
    // crossbar.io.in(port) <> inputBuffers(port)(0).io.out
    crossbar.io.in(port) <> inputBuffers(port).io.out
    crossbar.io.select(port) := routeDecisions(port)
  }

  // Connect Crossbar outputs to router output ports
  for (port <- 0 until config.portNum) {
    io.outPorts(port) <> crossbar.io.out(port)
  }
}

/**
 * RouterBuilder - Router builder
 * Provides convenient methods to build different types of routers
 */
object RouterBuilder {
  /**
   * Build a router using XY routing
   */
  def buildXYRouter(config: NoCConfig, meshWidth: Int, meshHeight: Int): Router = {
    val routingPolicy: RoutingPolicy = new XYRouting(config, meshWidth, meshHeight)
    Module(new Router(config, routingPolicy))
  }

  /**
   * Build a router using adaptive XY routing
   */
  def buildAdaptiveXYRouter(config: NoCConfig, meshWidth: Int, meshHeight: Int): Router = {
    val routingPolicy: RoutingPolicy = new AdaptiveXYRouting(config, meshWidth, meshHeight)
    Module(new Router(config, routingPolicy))
  }

  /**
   * Build a router using custom routing policy
   */
  def buildRouter(config: NoCConfig, routingPolicy: RoutingPolicy): Router = {
    Module(new Router(config, routingPolicy))
  }
}
