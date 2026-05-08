package bus.simple

import chisel3._

class SimpleBus(w: Int) extends Bundle {
  val addr = UInt(w.W)
  val data = UInt(w.W)
  val we   = Bool()
}
