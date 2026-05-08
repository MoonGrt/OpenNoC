package bus.amba.axi.axifull

import bus.amba.axi.common._
import chisel3._
import chisel3.util._

/** 1:1 link between an AXI master and an AXI slave. */
class AxiCrossbar11(p: AxiParams) extends Module {
  val io = IO(new Bundle {
    val fromMaster = new AxiSlavePort(p)
    val toSlave    = new AxiMasterPort(p)
  })

  io.toSlave.aw <> io.fromMaster.aw
  io.toSlave.w  <> io.fromMaster.w
  io.fromMaster.b <> io.toSlave.b
  io.toSlave.ar <> io.fromMaster.ar
  io.fromMaster.r <> io.toSlave.r
}
