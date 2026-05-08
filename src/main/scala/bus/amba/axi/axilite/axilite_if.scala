package bus.amba.axi.axilite

import bus.amba.axi.common._
import chisel3._
import chisel3.util._

class AxiLiteWriteAddr(val p: AxiLiteParams) extends Bundle {
  val addr = UInt(p.addrBits.W)
  val prot = UInt(3.W)
}

class AxiLiteWriteData(val p: AxiLiteParams) extends Bundle {
  val data = UInt(p.dataBits.W)
  val strb = UInt(p.strobeBits.W)
}

class AxiLiteWriteResp extends Bundle {
  val resp = UInt(2.W)
}

class AxiLiteReadAddr(val p: AxiLiteParams) extends Bundle {
  val addr = UInt(p.addrBits.W)
  val prot = UInt(3.W)
}

class AxiLiteReadData(val p: AxiLiteParams) extends Bundle {
  val data = UInt(p.dataBits.W)
  val resp = UInt(2.W)
}

class AxiLiteMasterPort(val p: AxiLiteParams) extends Bundle {
  val aw = Decoupled(new AxiLiteWriteAddr(p))
  val w  = Decoupled(new AxiLiteWriteData(p))
  val b  = Flipped(Decoupled(new AxiLiteWriteResp))
  val ar = Decoupled(new AxiLiteReadAddr(p))
  val r  = Flipped(Decoupled(new AxiLiteReadData(p)))
}

class AxiLiteSlavePort(val p: AxiLiteParams) extends Bundle {
  val aw = Flipped(Decoupled(new AxiLiteWriteAddr(p)))
  val w  = Flipped(Decoupled(new AxiLiteWriteData(p)))
  val b  = Decoupled(new AxiLiteWriteResp)
  val ar = Flipped(Decoupled(new AxiLiteReadAddr(p)))
  val r  = Decoupled(new AxiLiteReadData(p))
}
