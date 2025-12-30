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
class UniPipelineChannel(config: NoCConfig) extends UniNoCChannel(config) {
  val flitReg = Reg(new Flit(config.flitConfig))
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
class BiPipelineChannel(config: NoCConfig) extends BiNoCChannel(config) {
  // tx
  val txflitReg = Reg(new Flit(config.flitConfig))
  val txvalidReg = RegInit(false.B)
  // rx
  val rxflitReg = Reg(new Flit(config.flitConfig))
  val rxvalidReg = RegInit(false.B)

  // Can receive new data when output is ready or invalid
  // tx
  when(io.tx.out.ready || !txvalidReg) {
    txflitReg := io.tx.in.bits
    txvalidReg := io.tx.in.valid
  }
  io.tx.in.ready := io.tx.out.ready || !txvalidReg
  io.tx.out.bits := txflitReg
  io.tx.out.valid := txvalidReg
  // rx
  when(io.rx.out.ready || !rxvalidReg) {
    rxflitReg := io.rx.in.bits
    rxvalidReg := io.rx.in.valid
  }
  io.rx.in.ready := io.rx.out.ready || !rxvalidReg
  io.rx.out.bits := rxflitReg
  io.rx.out.valid := rxvalidReg
}
