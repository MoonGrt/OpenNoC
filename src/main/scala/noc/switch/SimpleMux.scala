package noc.switch

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig
import noc.arbiter.{NoCArbiter, RoundRobin}

/**
 * SimpleMux - Simple multiplexer
 * Each output port uses an arbiter to select from multiple inputs
 * Supports many-to-one mapping (multiple inputs can compete for the same output)
 *
 * @param config NoC configuration
 * @param numInputs Number of inputs
 * @param numOutputs Number of outputs
 */
class SimpleMux(config: NoCConfig, numInputs: Int, numOutputs: Int) extends SwitchFabric(config, numInputs, numOutputs) {
  import config._

  // Create arbiter for each output port
  val arbiters = Seq.fill(numOutputs) {
    Module(new RoundRobin(config, numInputs))
  }

  // Route inputs to corresponding arbiters based on select signal
  for (outIdx <- 0 until numOutputs) {
    // Collect all inputs that select this output
    val selectedInputs = Wire(Vec(numInputs, Decoupled(new Flit(flitConfig))))

    for (inIdx <- 0 until numInputs) {
      val selected = io.select(inIdx) === outIdx.U
      selectedInputs(inIdx).valid := io.in(inIdx).valid && selected
      selectedInputs(inIdx).bits := io.in(inIdx).bits
      io.in(inIdx).ready := selected && arbiters(outIdx).io.in(inIdx).ready
    }

    // Connect arbiter
    arbiters(outIdx).io.in <> selectedInputs
    io.out(outIdx) <> arbiters(outIdx).io.out
  }
}
