package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{BiNoCChannel, BiPipelineChannel}

/**
 * TorusTopology - 2D Torus topology
 * Nodes arranged in a grid, each node connected to four neighbors (up, down, left, right)
 * Edges wrap around (torus) in both X and Y directions
 *
 * @param config NoC configuration
 * @param width Torus width (number of nodes in X direction)
 * @param height Torus height (number of nodes in Y direction)
 */
class TorusTopology(config: NoCConfig, val width: Int, val height: Int) extends NoCTopology(config) {
  require(width > 0 && height > 0, "Torus dimensions must be positive")
  require(config.portNum >= 4, "Torus topology requires at least 4 ports")

  override def numNodes: Int = width * height

  /**
   * Convert node ID to coordinates
   */
  private def nodeIdToCoord(nodeId: Int): (Int, Int) = {
    val x = nodeId % width
    val y = nodeId / width
    (x, y)
  }

  /**
   * Convert coordinates to node ID
   */
  private def coordToNodeId(x: Int, y: Int): Int = {
    val wrappedX = (x + width) % width
    val wrappedY = (y + height) % height
    wrappedY * width + wrappedX
  }

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.port] = {
    val (srcX, srcY) = nodeIdToCoord(srcNodeId)
    val (dstX, dstY) = nodeIdToCoord(dstNodeId)

    val dx = (dstX - srcX + width) % width
    val dy = (dstY - srcY + height) % height

    if (dx == 1 && dy == 0) Some(Port.East)
    else if (dx == width - 1 && dy == 0) Some(Port.West) // wrap around
    else if (dx == 0 && dy == 1) Some(Port.North)
    else if (dx == 0 && dy == height - 1) Some(Port.South) // wrap around
    else None
  }

  override def getNeighbors(nodeId: Int): Seq[(Int, Port.port)] = {
    val (x, y) = nodeIdToCoord(nodeId)
    Seq(
      (coordToNodeId(x - 1, y), Port.West),  // wrap left
      (coordToNodeId(x + 1, y), Port.East),  // wrap right
      (coordToNodeId(x, y - 1), Port.South), // wrap down
      (coordToNodeId(x, y + 1), Port.North)  // wrap up
    )
  }

  override def connectRouters(routers: Seq[RouterIO]): Unit = {
    require(routers.length == numNodes, s"Expected ${numNodes} routers, got ${routers.length}")

    // 1. Disable all ports initially
    for (r <- routers) {
      for (p <- Port.dirEWNS) { // East/West/North/South
        r.inPorts(p.id).valid := false.B
        r.inPorts(p.id).bits  := 0.U.asTypeOf(r.inPorts(p.id).bits)
        r.outPorts(p.id).ready := false.B
      }
    }

    // 2. Establish bidirectional torus connections
    val channels = collection.mutable.Map[(Int, Int), BiNoCChannel]()

    for (nodeId <- 0 until numNodes) {
      val neighbors = getNeighbors(nodeId)

      for ((neighborId, _) <- neighbors) {
        val channelKey =
          if (nodeId < neighborId) (nodeId, neighborId)
          else (neighborId, nodeId)

        if (!channels.contains(channelKey)) {
          val channel = Module(new BiPipelineChannel(config))
          channels(channelKey) = channel

          val (a, b) = channelKey
          val (aOut, bIn) = getConnection(a, b) match {
            case Some(p) => (p, Port.opposite(p))
            case None    => throw new IllegalArgumentException("Invalid connection")
          }
          val (bOut, aIn) = getConnection(b, a) match {
            case Some(p) => (p, Port.opposite(p))
            case None    => throw new IllegalArgumentException("Invalid connection")
          }

          routers(a).outPorts(aIn.id) <> channel.io.rx.in
          channel.io.rx.out <> routers(b).inPorts(bOut.id)
          routers(b).outPorts(bIn.id) <> channel.io.tx.in
          channel.io.tx.out <> routers(a).inPorts(aOut.id)
        }
      }
    }
  }
}
