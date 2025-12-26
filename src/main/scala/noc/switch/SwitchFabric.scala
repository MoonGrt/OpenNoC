package noc.switch

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * SwitchFabric - Base interface for switch fabric
 * Implements switching from multiple inputs to multiple outputs
 */
abstract class SwitchFabric(val config: NoCConfig, val numInputs: Int, val numOutputs: Int) extends Module {
  require(numInputs > 0, "Number of inputs must be positive")
  require(numOutputs > 0, "Number of outputs must be positive")

  val io = IO(new Bundle {
    val in = Flipped(Vec(numInputs, Decoupled(new Flit(config))))
    val out = Vec(numOutputs, Decoupled(new Flit(config)))
    val select = Input(Vec(numInputs, UInt(chisel3.util.log2Ceil(numOutputs).W)))  // Each input selects output port
  })
}
