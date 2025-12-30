package noc.data

import chisel3._
import chisel3.util._
import noc.config.{FlitConfig, FlitType}

/**
 * Flit - Basic transmission unit in the network
 *
 * All header fields are packed into a single UInt:
 * [flitType | vcId | srcId | dstId]
 *
 * @param config NoC configuration
 */
class Flit(val config: FlitConfig) extends Bundle {
  import config._

  /** Interface for the entire header */
  val flit = UInt(totalWidth.W)  // Header and data combined

  println(s"Flit total width: $totalWidth")
  println(s"Flit header width: $headerWidth, bit: (${totalWidth-1} downto ${dataWidth})")
  println(s"flitType width: $typeWidth, header bit: (${headerWidth-1} downto ${headerWidth-typeWidth})")
  println(s"vcId width: ${vcIdWidth}, header bit: (${headerWidth-typeWidth-1} downto ${headerWidth-typeWidth-vcIdWidth})")
  println(s"dstId width: ${dstIdWidth}, header bit: (${dstIdWidth-1} downto 0)")
  println(s"data width: ${dataWidth}, bit: (${dataWidth-1} downto 0)")

  /* Extract header */
  def header: UInt = flit(totalWidth-1, dataWidth)
  /** Extract data */
  def data: UInt = flit(dataWidth-1, headerWidth)

  /** Extract flit type (highest 2 bits) */
  def flitType: UInt = header(headerWidth-1, headerWidth-typeWidth)
  /** Extract virtual channel ID */
  def vcId: UInt = header(headerWidth-typeWidth-1, headerWidth-typeWidth-vcIdWidth)
  /** Extract destination node ID (lowest nodeIdWidth bits) */
  def dstId: UInt = header(dstIdWidth-1, 0)

  /** Check if this is a head flit */
  def isHead: Bool = flitType === FlitType.Head.id.U || flitType === FlitType.HeadTail.id.U
  /** Check if this is a tail flit */
  def isTail: Bool = flitType === FlitType.Tail.id.U || flitType === FlitType.HeadTail.id.U
  /** Check if this is a body flit */
  def isBody: Bool = flitType === FlitType.Body.id.U
  /** Check if this is a single-flit packet (head-tail) */
  def isHeadTail: Bool = flitType === FlitType.HeadTail.id.U
}

object Flit {
  /**
   * Utility method to pack fields into a single UInt
   */
  def pack(flitType: UInt, vcId: UInt, srcId: UInt, dstId: UInt, data: UInt, config: FlitConfig): UInt = {
    Cat(flitType, vcId, srcId, dstId, data)
  }

  /**
   * Create a head flit
   */
  def head(config: FlitConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := this.pack(FlitType.Head.id.U, vcId, srcId, dstId, data, config)
    flit
  }

  /**
   * Create a body flit
   */
  def body(config: FlitConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    // Source and destination IDs are not used for body flits
    flit.flit := this.pack(FlitType.Body.id.U, vcId, 0.U, 0.U, data, config)
    flit
  }

  /**
   * Create a tail flit
   */
  def tail(config: FlitConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    // Source and destination IDs are not used for tail flits
    flit.flit := this.pack(FlitType.Tail.id.U, vcId, 0.U, 0.U, data, config)
    flit
  }

  /**
   * Create a single-flit packet (head-tail)
   */
  def headTail(config: FlitConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := this.pack(FlitType.HeadTail.id.U, vcId, srcId, dstId, data, config)
    flit
  }

  /**
   * Create an empty flit
   */
  def empty(config: FlitConfig): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := 0.U
    flit
  }
}
