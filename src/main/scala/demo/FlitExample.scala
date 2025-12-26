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
    flitWidth = 32,
    vcNum = 2,
    bufferDepth = 4,
    nodeIdWidth = 8,
    numPorts = 4
  )

  // Example 1: Create a head flit
  val headFlit = Flit.head(
    config = config,
    srcId = 0.U,
    dstId = 5.U,
    data = 0x12345678.U,
    vcId = 0.U
  )

  // Example 2: Create a body flit
  val bodyFlit = Flit.body(
    config = config,
    data = 0xABCDEF00.U,
    vcId = 0.U
  )

  // Example 3: Create a tail flit
  val tailFlit = Flit.tail(
    config = config,
    data = 0xFFFFFFFF.U,
    vcId = 0.U
  )

  // Example 4: Create a single-flit packet (headTail)
  val singleFlit = Flit.headTail(
    config = config,
    srcId = 0.U,
    dstId = 5.U,
    data = 0xDEADBEEF.U,
    vcId = 0.U
  )

  // Example 5: Create an empty flit
  val emptyFlit = Flit.empty(config)

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
    data = 0xCAFEBABE.U,
    vcId = 0.U
  )
}
