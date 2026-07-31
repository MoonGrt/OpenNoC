package noc.config

import chisel3._
import chisel3.util._

/**
 * HeaderType - Flit header field enumeration
 */
object HeaderType extends Enumeration {
  type HeaderType = Value

  val FlitType = Value(0)
  val VcId     = Value(1)
  val SrcId    = Value(2)
  val DstId    = Value(3)
  val QoS      = Value(4)
  val User     = Value(5)

  /** number of flit types */
  val size: Int = values.size
  /** bit width required to encode flit type */
  val width: Int = log2Ceil(values.size)
}

/**
 * HeaderField - describes a field inside a flit header
 */
case class HeaderField(
  Type: HeaderType.Value,
  Width: Int,
)

/**
 * HeaderBits - describes the bits of fields inside a flit header
 */
case class HeaderBits(
  Type: HeaderType.Value,
  Width: Int,
  Offset: Int,
)


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
 * @param fields Sequence of header fields
 * @param dataWidth Data bit width
 */
case class FlitConfig(
  headerFields: Seq[HeaderField],
  dataWidth: Int
) {
  /** total header width */
  val headerWidth: Int =
    headerFields.map { f => f.Width }.sum

  /** total flit width */
  val totalWidth: Int =
    headerWidth + dataWidth

  /** compute bits (header-local bit positions) */
  val bits: Seq[HeaderBits] = {
    var offset = headerWidth
    headerFields.map { f =>
      offset -= f.Width
      HeaderBits(f.Type, f.Width, offset)
    }
  }

  /** map of header types to header bits */
  val bitsMap: Map[HeaderType.Value, HeaderBits] =
    bits.map(b => b.Type -> b).toMap
}
