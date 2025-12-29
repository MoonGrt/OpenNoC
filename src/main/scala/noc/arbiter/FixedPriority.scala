package noc.arbiter

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * FixedPriority - Fixed priority arbiter
 * Input 0 has highest priority, input numInputs-1 has lowest priority
 *
 * @param config NoC configuration
 * @param numInputs Number of inputs
 */
class FixedPriority(config: NoCConfig, numInputs: Int) extends NoCArbiter(config, numInputs) {
  val arbiter = Module(new chisel3.util.Arbiter(new Flit(config), numInputs))

  // Connect inputs
  for (i <- 0 until numInputs) { arbiter.io.in(i) <> io.in(i) }
  // Connect output
  io.out <> arbiter.io.out
  io.chosen := arbiter.io.chosen
}
