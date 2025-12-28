package noc.router

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.{NoCConfig, Port}
import noc.routing.RoutingPolicy
import noc.switch.{SwitchFabric, Crossbar}
import noc.channel.BufferedChannel

/**
 * Router - Router
 * Core component in NoC, responsible for routing and forwarding flits
 *
 * @param config NoC configuration
 * @param routingPolicy Routing policy
 */
class Router(val config: NoCConfig, val routingPolicy: RoutingPolicy) extends Module {
  val io = IO(new RouterIO(config))

  // Input buffers: create buffers for each input port and each VC
  val inputBuffers = Seq.fill(config.totalPorts) {
    Seq.fill(config.vcNum) {
      Module(new BufferedChannel(config, config.bufferDepth))
    }
  }

  // Connect input ports to buffers
  for (port <- 0 until config.totalPorts) {
    // Simplified: assume only one VC, should route to corresponding VC based on flit's vcId
    inputBuffers(port)(0).io.in <> io.inPorts(port)
  }

  // Route computation: compute output port for each input port
  val routeDecisions = Wire(Vec(config.totalPorts, UInt(config.portWidth.W)))
  val routeValids = Wire(Vec(config.totalPorts, Bool()))

  for (port <- 0 until config.totalPorts) {
    val flit = inputBuffers(port)(0).io.out.bits
    val hasFlit = inputBuffers(port)(0).io.out.valid

    // Compute routing decision (only compute for head flit, body and tail flits use previous routing decision)
    val routeDecision = routingPolicy.route(io.routerId, flit.dstId)
    routeDecisions(port) := routeDecision
    routeValids(port) := hasFlit && flit.isHead
  }

  // Switch fabric: implemented using Crossbar
  val crossbar = Module(new Crossbar(config, config.totalPorts, config.totalPorts))

  // Connect buffer outputs to Crossbar inputs
  for (port <- 0 until config.totalPorts) {
    crossbar.io.in(port) <> inputBuffers(port)(0).io.out
    crossbar.io.select(port) := routeDecisions(port)
  }

  // Connect Crossbar outputs to router output ports
  for (port <- 0 until config.totalPorts) {
    io.outPorts(port) <> crossbar.io.out(port)
  }
}
