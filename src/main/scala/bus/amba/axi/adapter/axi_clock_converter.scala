package bus.amba.axi.adapter

import bus.amba.axi.common._
import chisel3._
import chisel3.util._

/** Same-clock pass-through (insert register slices later for CDC). */
class AxiClockConverter(p: AxiParams) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(new AxiSlavePort(p))
    val out = new AxiSlavePort(p)
  })
  io.out <> io.in
}
