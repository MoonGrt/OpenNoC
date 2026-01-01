package noc.data

import chisel3._
import chisel3.util._
import noc.config.{FlitConfig, FlitType, HeaderType}

/**
 * Flit - Basic transmission unit in the network
 *
 * @param config NoC configuration
 */
class Flit(val config: FlitConfig) extends Bundle {
  import config._

  /** Interface for the entire header */
  val flit = UInt(totalWidth.W)  // Header and data combined

  /* Extract header */
  def header: UInt = flit(totalWidth-1, dataWidth)
  /** Extract data */
  def data: UInt = flit(dataWidth-1, headerWidth)

  /** Extract flit type (highest 2 bits) */
  def flitType: UInt = getField(HeaderType.FlitType)
  /** Extract virtual channel ID */
  def vcId: UInt = getField(HeaderType.VcId)
  /** Extract source node ID (lowest nodeIdWidth bits) */
  def srcId: UInt = getField(HeaderType.SrcId)
  /** Extract destination node ID (lowest nodeIdWidth bits) */
  def dstId: UInt = getField(HeaderType.DstId)

  /** Check if this is a head flit */
  def isHead: Bool = flitType === FlitType.Head.id.U || flitType === FlitType.HeadTail.id.U
  /** Check if this is a tail flit */
  def isTail: Bool = flitType === FlitType.Tail.id.U || flitType === FlitType.HeadTail.id.U
  /** Check if this is a body flit */
  def isBody: Bool = flitType === FlitType.Body.id.U
  /** Check if this is a single-flit packet (head-tail) */
  def isHeadTail: Bool = flitType === FlitType.HeadTail.id.U

  /** extract Header bits (Returns 0 if it does not exist.) */
  def getField(t: HeaderType.Value): UInt = {
    require(config.bitsMap.contains(t), s"Header $t not present in this flit format")

    val width = config.bitsMap(t).Width
    if (width == 0) {
      0.U
    } else {
      val offset = config.bitsMap(t).Offset
      header(offset + width - 1, offset)
    }
  }
}

object Flit {
  /**
   * Generic pack method based on FlitConfig
   */
  private def packWithConfig(
    config: FlitConfig,
    fields: Map[HeaderType.Value, UInt],
    data: UInt
  ): UInt = {
    val header = fields.foldLeft(0.U(config.headerWidth.W)) {
      case (acc, (t, value)) =>
        require(
          config.bitsMap.contains(t),
          s"Header field $t not present in this FlitConfig"
        )
        val bits = config.bitsMap(t)
        val masked = if (bits.Width > 0) {
          value(bits.Width - 1, 0) << bits.Offset
        } else {
          0.U
        }
        acc | masked
    }
    Cat(header, data)
  }

  /**
   * Create a head flit
   */
  def head(config: FlitConfig, vcId: UInt = 0.U, srcId: UInt, dstId: UInt, data: UInt): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := packWithConfig(
      config,
      Map(
        HeaderType.FlitType -> FlitType.Head.id.U,
        HeaderType.VcId     -> vcId,
        HeaderType.SrcId    -> srcId,
        HeaderType.DstId    -> dstId
      ),
      data
    )
    flit
  }

  /**
   * Create a body flit
   */
  def body(config: FlitConfig, vcId: UInt = 0.U, data: UInt): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := packWithConfig(
      config,
      Map(
        HeaderType.FlitType -> FlitType.Body.id.U,
        HeaderType.VcId     -> vcId
      ),
      data
    )
    flit
  }

  /**
   * Create a tail flit
   */
  def tail(config: FlitConfig, vcId: UInt = 0.U, data: UInt): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := packWithConfig(
      config,
      Map(
        HeaderType.FlitType -> FlitType.Tail.id.U,
        HeaderType.VcId     -> vcId
      ),
      data
    )
    flit
  }

  /**
   * Create a single-flit packet (head-tail)
   */
  def headTail(config: FlitConfig, vcId: UInt = 0.U, srcId: UInt, dstId: UInt, data: UInt): Flit = {
    val flit = Wire(new Flit(config))
    flit.flit := packWithConfig(
      config,
      Map(
        HeaderType.FlitType -> FlitType.HeadTail.id.U,
        HeaderType.VcId     -> vcId,
        HeaderType.SrcId    -> srcId,
        HeaderType.DstId    -> dstId
      ),
      data
    )
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
