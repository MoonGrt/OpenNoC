package noc.config

import chisel3._
import chisel3.util._

/**
 * FlitType - Flit type enumeration
 */
object FlitType extends Enumeration {
  type FlitType = Value
  val Body    = Value(0)   // Body flit
  val Head    = Value(1)   // Head flit
  val Tail    = Value(2)   // Tail flit
  val HeadTail = Value(3)  // Single-flit packet (both head and tail)
  /** number of flit types */
  val size: Int = values.size
  /** bit width required to encode flit type */
  val width: Int = log2Ceil(size)
}

/**
 * FlitConfig - describes fields inside a flit
 * 
 * @param vcIdWidth  Virtual channel ID width
 * @param dstIdWidth Destination ID width
 * @param dataWidth  Data width
 */
case class FlitConfig(
  vcIdWidth: Int,
  dstIdWidth: Int,
  dataWidth: Int
) {
  /** flit type width */
  val typeWidth: Int = FlitType.width

  /** header width (without data) */
  val headerWidth: Int = typeWidth + vcIdWidth + dstIdWidth

  /** total flit width */
  val totalWidth: Int = headerWidth + dataWidth
}
