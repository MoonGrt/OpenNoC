package noc.arbiter

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * NoCArbiter - Base interface for arbiters
 * Used for arbitration when multiple inputs compete for one output
 */
abstract class NoCArbiter(val config: NoCConfig, val numInputs: Int) extends Module {
  require(numInputs > 0, "Number of inputs must be positive")

  val io = IO(new Bundle {
    val in = Flipped(Vec(numInputs, Decoupled(new Flit(config))))
    val out = Decoupled(new Flit(config))
    val chosen = Output(UInt(chisel3.util.log2Ceil(numInputs).W))  // Selected input index
  })
}
