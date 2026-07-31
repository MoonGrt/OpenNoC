package opennoc.noc.data

import spinal.core._

case class PacketBeat(dataWidth: Int, nodeIdWidth: Int) extends Bundle {
  val data = Bits(dataWidth bits)
  val dest = UInt(nodeIdWidth bits)
  val last = Bool()
}
