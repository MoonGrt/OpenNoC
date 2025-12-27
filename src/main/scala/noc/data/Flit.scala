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
 * @param config NoC configuration
 */
class Flit(val config: NoCConfig) extends Bundle {
  val flitType = UInt(2.W)  // Flit type
  val vcId     = UInt(config.vcIdWidth.W)  // Virtual channel ID
  val srcId    = UInt(config.nodeIdWidth.W)  // Source node ID
  val dstId    = UInt(config.nodeIdWidth.W)  // Destination node ID
  val data     = UInt(config.flitWidth.W)  // Data

  /**
   * Check if this is a head flit
   */
  def isHead: Bool = flitType === FlitType.Head.id.U || flitType === FlitType.HeadTail.id.U

  /**
   * Check if this is a tail flit
   */
  def isTail: Bool = flitType === FlitType.Tail.id.U || flitType === FlitType.HeadTail.id.U

  /**
   * Check if this is a body flit
   */
  def isBody: Bool = flitType === FlitType.Body.id.U

  /**
   * Check if this is a single-flit packet
   */
  def isHeadTail: Bool = flitType === FlitType.HeadTail.id.U
}

object Flit {
  /**
   * Create a head flit
   */
  def head(config: NoCConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flitType := FlitType.Head.id.U
    flit.vcId := vcId
    flit.srcId := srcId
    flit.dstId := dstId
    flit.data := data
    flit
  }

  /**
   * Create a body flit
   */
  def body(config: NoCConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flitType := FlitType.Body.id.U
    flit.vcId := vcId
    flit.srcId := 0.U
    flit.dstId := 0.U
    flit.data := data
    flit
  }

  /**
   * Create a tail flit
   */
  def tail(config: NoCConfig, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flitType := FlitType.Tail.id.U
    flit.vcId := vcId
    flit.srcId := 0.U
    flit.dstId := 0.U
    flit.data := data
    flit
  }

  /**
   * Create a single-flit packet
   */
  def headTail(config: NoCConfig, srcId: UInt, dstId: UInt, data: UInt, vcId: UInt = 0.U): Flit = {
    val flit = Wire(new Flit(config))
    flit.flitType := FlitType.HeadTail.id.U
    flit.vcId := vcId
    flit.srcId := srcId
    flit.dstId := dstId
    flit.data := data
    flit
  }

  /**
   * Create an empty flit
   */
  def empty(config: NoCConfig): Flit = {
    val flit = Wire(new Flit(config))
    flit.flitType := 0.U
    flit.vcId := 0.U
    flit.srcId := 0.U
    flit.dstId := 0.U
    flit.data := 0.U
    flit
  }
}
