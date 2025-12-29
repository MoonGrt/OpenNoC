package noc.data

import chisel3._
import chisel3.util._
import noc.config.NoCConfig

/**
 * FlitType - Flit type enumeration
 */
object FlitType extends Enumeration {
  type FlitType = Value
  val Head    = Value(0)  // Head flit
  val Body    = Value(1)  // Body flit
  val Tail    = Value(2)  // Tail flit
  val HeadTail = Value(3) // Single-flit packet (both head and tail)
}

/**
 * Flit - Basic transmission unit in the network
 *
 * All header fields are packed into a single UInt:
 * [flitType | vcId | srcId | dstId]
 *
 * @param config NoC configuration
 */
class Flit(val config: NoCConfig) extends Bundle {
  // Total width = flitType(2) + vcId + srcId + dstId
  val typeWidth: Int = 2
  val totalWidth = typeWidth + config.vcIdWidth + 2 * config.nodeIdWidth

  /** Interface for the entire header */
  val header = UInt(totalWidth.W)  // Header
  val data   = UInt(config.flitWidth.W)  // Data

  // println(s"flitType width: $typeWidth, bit: (${totalWidth-1} downto ${totalWidth-typeWidth})")
  // println(s"vcId width: ${config.vcIdWidth}, bit: (${totalWidth-typeWidth-1} downto ${totalWidth-typeWidth-config.vcIdWidth})")
  // println(s"srcId width: ${config.nodeIdWidth}, bit: (${totalWidth-typeWidth-config.vcIdWidth-1} downto ${config.nodeIdWidth})")
  // println(s"dstId width: ${config.nodeIdWidth}, bit: (${config.nodeIdWidth-1} downto 0)")

  /** Extract flit type (highest 2 bits) */
  def flitType: UInt = header(totalWidth-1, totalWidth-typeWidth)
  /** Extract virtual channel ID */
  def vcId: UInt = header(totalWidth-typeWidth-1, totalWidth-typeWidth-config.vcIdWidth)
  /** Extract source node ID */
  def srcId: UInt = header(totalWidth-typeWidth-config.vcIdWidth-1, config.nodeIdWidth)
  /** Extract destination node ID (lowest nodeIdWidth bits) */
  def dstId: UInt = header(config.nodeIdWidth-1, 0)

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
  def pack(flitType: UInt, vcId: UInt, srcId: UInt, dstId: UInt, config: NoCConfig): UInt = {
    Cat(flitType, vcId, srcId, dstId)
  }

  /**
   * Create a head flit
   */
  def head(config: NoCConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.header := this.pack(FlitType.Head.id.U, vcId, srcId, dstId, config)
    flit.data := data
    flit
  }

  /**
   * Create a body flit
   */
  def body(config: NoCConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    // Source and destination IDs are not used for body flits
    flit.header := this.pack(FlitType.Body.id.U, vcId, 0.U, 0.U, config)
    flit.data := data
    flit
  }

  /**
   * Create a tail flit
   */
  def tail(config: NoCConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    // Source and destination IDs are not used for tail flits
    flit.header := this.pack(FlitType.Tail.id.U, vcId, 0.U, 0.U, config)
    flit.data := data
    flit
  }

  /**
   * Create a single-flit packet (head-tail)
   */
  def headTail(config: NoCConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.header := this.pack(FlitType.HeadTail.id.U, vcId, srcId, dstId, config)
    flit.data := data
    flit
  }

  /**
   * Create an empty flit
   */
  def empty(config: NoCConfig): Flit = {
    val flit = Wire(new Flit(config))
    flit.header := 0.U
    flit.data := 0.U
    flit
  }
}
