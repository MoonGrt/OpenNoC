package bus.amba.axi.common

import chisel3._
import chisel3.util._

class AxiAddrChannel(val p: AxiParams) extends Bundle {
  val id     = UInt(p.idBits.W)
  val addr   = UInt(p.addrBits.W)
  val len    = UInt(8.W)
  val size   = UInt(3.W)
  val burst  = UInt(2.W)
  val lock   = UInt(1.W)
  val cache  = UInt(4.W)
  val prot   = UInt(3.W)
  val qos    = UInt(4.W)
  val region = UInt(4.W)
}

class AxiDataWriteChannel(val p: AxiParams) extends Bundle {
  val data = UInt(p.dataBits.W)
  val strb = UInt(p.strobeBits.W)
  val last = Bool()
}

class AxiWriteRespChannel(val p: AxiParams) extends Bundle {
  val id   = UInt(p.idBits.W)
  val resp = UInt(2.W)
}

class AxiDataReadChannel(val p: AxiParams) extends Bundle {
  val id   = UInt(p.idBits.W)
  val data = UInt(p.dataBits.W)
  val resp = UInt(2.W)
  val last = Bool()
}

class AxiMasterPort(val p: AxiParams) extends Bundle {
  val aw = Decoupled(new AxiAddrChannel(p))
  val w  = Decoupled(new AxiDataWriteChannel(p))
  val b  = Flipped(Decoupled(new AxiWriteRespChannel(p)))
  val ar = Decoupled(new AxiAddrChannel(p))
  val r  = Flipped(Decoupled(new AxiDataReadChannel(p)))
}

class AxiSlavePort(val p: AxiParams) extends Bundle {
  val aw = Flipped(Decoupled(new AxiAddrChannel(p)))
  val w  = Flipped(Decoupled(new AxiDataWriteChannel(p)))
  val b  = Decoupled(new AxiWriteRespChannel(p))
  val ar = Flipped(Decoupled(new AxiAddrChannel(p)))
  val r  = Decoupled(new AxiDataReadChannel(p))
}
