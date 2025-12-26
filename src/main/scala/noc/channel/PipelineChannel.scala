package noc.channel

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * PipelineChannel - Pipeline channel
 * Inserts a register stage between input and output, providing one pipeline delay
 *
 * @param config NoC configuration
 */
class PipelineChannel(config: NoCConfig) extends NoCChannel(config) {
  val flitReg = Reg(new Flit(config))
  val validReg = RegInit(false.B)

  // Can receive new data when output is ready or invalid
  when(io.out.ready || !validReg) {
    flitReg := io.in.bits
    validReg := io.in.valid
  }

  io.in.ready := io.out.ready || !validReg
  io.out.bits := flitReg
  io.out.valid := validReg
}
