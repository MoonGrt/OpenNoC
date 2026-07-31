package amba

import amba.apb._
import amba.axi.adapter._
import amba.axi.axifull._
import amba.axi.axilite._
import amba.axi.axistream._
import amba.axi.bridge._
import amba.axi.common._
import amba.axi.host._
import amba.ahb._
import chisel3._
import circt.stage.ChiselStage
import org.scalatest.flatspec.AnyFlatSpec

/** Elaboration-only checks (no Verilator required). */
class AmbaElaborationSpec extends AnyFlatSpec {

  def elaborate(m: => RawModule): Unit = ChiselStage.emitCHIRRTL(m)

  val apb  = ApbParams(16, 32)
  val lite = AxiLiteParams(16, 32)
  val full = AxiParams(16, 32, idBits = 4)
  val axis = AxisParams(32, 4, 4, 4)
  val ahb  = AhbParams(16, 32)

  "ApbRegSlave" should "elaborate" in {
    elaborate(new ApbRegSlave(apb, 32))
  }

  "ApbDecoder" should "elaborate" in {
    elaborate(new ApbDecoder(apb, Seq(BigInt(0), BigInt(0x100)), Seq(BigInt(0x100), BigInt(0x100))))
  }

  "ApbExampleSubsystem" should "elaborate" in {
    elaborate(new ApbExampleSubsystem(apb))
  }

  "AxiLiteRegSlave" should "elaborate" in {
    elaborate(new AxiLiteRegSlave(lite, 32))
  }

  "AxiLiteRegIf" should "elaborate" in {
    elaborate(new AxiLiteRegIf(lite, nCtl = 8))
  }

  "AxiLiteToApbBridge" should "elaborate" in {
    elaborate(new AxiLiteToApbBridge(apb))
  }

  "AxiLiteToAxi4" should "elaborate" in {
    elaborate(new AxiLiteToAxi4(lite, full))
  }

  "AxiCrossbar11" should "elaborate" in {
    elaborate(new AxiCrossbar11(full))
  }

  "AxiArbiter2" should "elaborate" in {
    elaborate(new AxiArbiter2(full))
  }

  "AxiIdRemap" should "elaborate" in {
    elaborate(new AxiIdRemap(AxiParams(16, 32, 8), AxiParams(16, 32, 4)))
  }

  "AxisFifo" should "elaborate" in {
    elaborate(new AxisFifo(axis, depth = 4))
  }

  "AhbRegSlave" should "elaborate" in {
    elaborate(new AhbRegSlave(ahb, 32))
  }

  "AxiLiteMasterHost" should "elaborate" in {
    elaborate(new AxiLiteMasterHost(lite))
  }

  "Axi4SingleBeatMasterHost" should "elaborate" in {
    elaborate(new Axi4SingleBeatMasterHost(full))
  }

  "ApbMasterHost" should "elaborate" in {
    elaborate(new ApbMasterHost(apb))
  }
}
