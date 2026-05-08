package bus.amba.axi.axifull

import bus.amba.axi.common._
import chisel3._
import chisel3.util._

/**
  * Placeholder two-to-one AXI4 mux: currently passes only `in(0)` through.
  * Replace with a full channel-aware arbiter when consolidating multiple masters.
  */
class AxiArbiter2(p: AxiParams) extends Module {
  val io = IO(new Bundle {
    val in  = Vec(2, Flipped(new AxiMasterPort(p)))
    val out = new AxiMasterPort(p)
  })

  io.out <> io.in(0)

  io.in(1).aw.ready := false.B
  io.in(1).w.ready  := false.B
  io.in(1).ar.ready := false.B
  io.in(1).b.bits   := DontCare
  io.in(1).b.valid  := false.B
  io.in(1).r.bits   := DontCare
  io.in(1).r.valid  := false.B
}
