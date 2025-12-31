package noc.data

import chisel3._
import chisel3.util._
import noc.config.NoCConfig

/**
 * Packet - Data packet
 * Consists of multiple flits, the basic logical unit for transmission in NoC
 *
 * @param config NoC configuration
 * @param maxFlits Maximum number of flits
 */
class Packet(val config: NoCConfig, val maxFlits: Int = 8) extends Bundle {
  val flits = Vec(maxFlits, new Flit(config.flitConfig))
  val length = UInt(chisel3.util.log2Ceil(maxFlits + 1).W)  // Actual number of flits
  val valid = Bool()

  /**
   * Get the head flit
   */
  def headFlit: Flit = flits(0)

  /**
   * Get the tail flit
   */
  def tailFlit: Flit = flits(length - 1.U)

  /**
   * Check if this is a single-flit packet
   */
  def isSingleFlit: Bool = length === 1.U && headFlit.isHeadTail
}

object Packet {
  /**
   * Create a packet from a sequence of flits
   */
  def fromFlits(config: NoCConfig, flitSeq: Seq[Flit], maxFlits: Int = 8): Packet = {
    val packet = Wire(new Packet(config, maxFlits))
    require(flitSeq.length <= maxFlits, s"Packet has ${flitSeq.length} flits, exceeds max ${maxFlits}")

    for (i <- 0 until maxFlits) {
      if (i < flitSeq.length) {
        packet.flits(i) := flitSeq(i)
      } else {
        packet.flits(i) := Flit.empty(config.flitConfig)
      }
    }
    packet.length := flitSeq.length.U
    packet.valid := true.B
    packet
  }

  /**
   * Create a single-flit packet
   */
  def singleFlit(config: NoCConfig, dstId: UInt, data: UInt, vcId: UInt = 0.U): Packet = {
    val packet = Wire(new Packet(config, 1))
    packet.flits(0) := Flit.headTail(config.flitConfig, dstId, data, vcId)
    packet.length := 1.U
    packet.valid := true.B
    packet
  }
}
