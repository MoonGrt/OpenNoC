package demo

import chisel3._
import noc.config.NoCConfig
import noc.data.{Flit, Packet}

/**
 * FlitExample - Example of creating and using flits
 *
 * This example demonstrates how to:
 * 1. Create different types of flits (head, body, tail, headTail)
 * 2. Create packets from flits
 * 3. Use flit helper methods
 */
class FlitExample extends Module {
  val config = NoCConfig(
    dataWidth = 32,
    vcNum = 2,
    bufferDepth = 4,
    nodeIdWidth = 8,
    numPorts = 4
  )

  // Example 1: Create a head flit
  val headFlit = Flit.head(
    config = config.flitConfig,
    srcId = 0.U,
    dstId = 5.U,
    data = "h12345678".U(32.W),
    vcId = 0.U
  )

  // Example 2: Create a body flit
  val bodyFlit = Flit.body(
    config = config.flitConfig,
    data = "hABCDEF00".U(32.W),
    vcId = 0.U
  )

  // Example 3: Create a tail flit
  val tailFlit = Flit.tail(
    config = config.flitConfig,
    data = "hFFFFFFFF".U(32.W),
    vcId = 0.U
  )

  // Example 4: Create a single-flit packet (headTail)
  val singleFlit = Flit.headTail(
    config = config.flitConfig,
    srcId = 0.U,
    dstId = 5.U,
    data = "hDEADBEEF".U(32.W),
    vcId = 0.U
  )

  // Example 5: Create an empty flit
  val emptyFlit = Flit.empty(config.flitConfig)

  // Example 6: Check flit types
  val isHead = headFlit.isHead
  val isTail = tailFlit.isTail
  val isBody = bodyFlit.isBody
  val isSingleFlit = singleFlit.isHeadTail

  // Example 7: Create a packet from flits
  val flitSeq = Seq(headFlit, bodyFlit, tailFlit)
  val packet = Packet.fromFlits(config, flitSeq, maxFlits = 8)

  // Example 8: Create a single-flit packet
  val singleFlitPacket = Packet.singleFlit(
    config = config,
    srcId = 0.U,
    dstId = 5.U,
    data = "hCAFEBABE".U(32.W),
    vcId = 0.U
  )
}



