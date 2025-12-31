package noc.switch

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig
import noc.arbiter._

/**
 * Crossbar - Crossbar switch
 * Implements fully connected switch fabric, supports mapping from any input to any output
 * Each output port uses an arbiter to handle contention from multiple inputs
 *
 * @param config NoC configuration
 * @param numInputs Number of inputs
 * @param numOutputs Number of outputs
 */
class Crossbar(config: NoCConfig, numInputs: Int, numOutputs: Int) extends SwitchFabric(config, numInputs, numOutputs) {
  import config._

  // Create arbiter for each output port
  val arbiters = Seq.fill(numOutputs) { config.routerConfig.switchArbiterType match {
      case "RoundRobin" => Module(new RoundRobin(config, numInputs))
      case "FixedPriority" => Module(new FixedPriority(config, numInputs))
      case _ => throw new IllegalArgumentException(s"Unknown arbiter type: ${routerConfig.vcConfig.arbiterType}")
    }
  }

  // Connect each input to all output arbiters
  for (outIdx <- 0 until numOutputs) {
    val selectedInputs = Wire(Vec(numInputs, Decoupled(new Flit(flitConfig))))

    for (inIdx <- 0 until numInputs) {
      val selected = io.select(inIdx) === outIdx.U
      selectedInputs(inIdx).valid := io.in(inIdx).valid && selected
      selectedInputs(inIdx).bits := io.in(inIdx).bits
    }

    // Handle ready signal: only pass ready when corresponding output is selected
    for (inIdx <- 0 until numInputs) {
      val selected = io.select(inIdx) === outIdx.U
      io.in(inIdx).ready := Mux(selected, arbiters(outIdx).io.in(inIdx).ready, false.B)
    }

    arbiters(outIdx).io.in <> selectedInputs
    io.out(outIdx) <> arbiters(outIdx).io.out
  }

  // Fix ready signal: each input's ready is the OR of all outputs that select it
  for (inIdx <- 0 until numInputs) {
    val readySignals = (0 until numOutputs).map { outIdx =>
      val selected = io.select(inIdx) === outIdx.U
      Mux(selected, arbiters(outIdx).io.in(inIdx).ready, false.B)
    }
    io.in(inIdx).ready := readySignals.reduce(_ || _)
  }
}
