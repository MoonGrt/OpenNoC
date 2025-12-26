package noc.arbiter

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * RoundRobin - Round-robin arbiter
 * Selects inputs in round-robin fashion, fairly distributes bandwidth
 *
 * @param config NoC configuration
 * @param numInputs Number of inputs
 */
class RoundRobin(config: NoCConfig, numInputs: Int) extends NoCArbiter(config, numInputs) {
  val arbiter = Module(new chisel3.util.RRArbiter(new Flit(config), numInputs))

  // Connect inputs
  for (i <- 0 until numInputs) {
    arbiter.io.in(i) <> io.in(i)
  }

  // Connect output
  io.out <> arbiter.io.out
  io.chosen := arbiter.io.chosen
}
