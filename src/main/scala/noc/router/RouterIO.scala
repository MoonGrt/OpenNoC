package noc.router

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * RouterIO - Router IO interface
 * Defines the input and output ports of a router
 *
 * @param config NoC configuration
 */
class RouterIO(val config: NoCConfig) extends Bundle {
  // Input ports (including Local port)
  val inPorts = Flipped(Vec(config.totalPorts, Decoupled(new Flit(config))))

  // Output ports (including Local port)
  val outPorts = Vec(config.totalPorts, Decoupled(new Flit(config)))

  // Router ID
  val routerId = Input(UInt(config.nodeIdWidth.W))

  // Optional credits for buffering
  // val incredits = Input(Vec(config.totalPorts, Bool()))
  // val increditsvcid = Input(Vec(config.totalPorts, UInt(config.vcIdWidth.W)))
  // val outcredits = Output(Vec(config.totalPorts, Bool()))
  // val outcreditsvcid = Output(Vec(config.totalPorts, UInt(config.vcIdWidth.W)))

  // Optional congestion information (for adaptive routing)
  val congestionInfo = Input(Vec(config.totalPorts, UInt(8.W)))  // Congestion level for each port
  // val congestionInfo = if (config.routingType == "Adaptive") {  // Congestion level for each port
  //   Some(Input(Vec(config.totalPorts, UInt(8.W))))
  // } else {
  //   None
  // }
}
