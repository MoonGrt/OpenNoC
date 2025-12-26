package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{NoCChannel, PipelineChannel}

/**
 * MeshTopology - 2D Mesh topology
 * Nodes arranged in a grid, each node connected to four neighbors (up, down, left, right)
 *
 * @param config NoC configuration
 * @param width Mesh width (number of nodes in X direction)
 * @param height Mesh height (number of nodes in Y direction)
 */
class MeshTopology(config: NoCConfig, val width: Int, val height: Int) extends NoCTopology(config) {
  require(width > 0 && height > 0, "Mesh dimensions must be positive")
  require(config.numPorts >= 4, "Mesh topology requires at least 4 ports")

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
    y * width + x
  }

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.Port] = {
    val (srcX, srcY) = nodeIdToCoord(srcNodeId)
    val (dstX, dstY) = nodeIdToCoord(dstNodeId)

    val dx = dstX - srcX
    val dy = dstY - srcY

    if (dx == 1 && dy == 0) {
      Some(Port.East)
    } else if (dx == -1 && dy == 0) {
      Some(Port.West)
    } else if (dx == 0 && dy == 1) {
      Some(Port.North)
    } else if (dx == 0 && dy == -1) {
      Some(Port.South)
    } else {
      None
    }
  }

  override def getNeighbors(nodeId: Int): Seq[(Int, Port.Port)] = {
    val (x, y) = nodeIdToCoord(nodeId)
    var neighbors = Seq.empty[(Int, Port.Port)]

    if (x > 0) {
      neighbors :+= (coordToNodeId(x - 1, y), Port.West)
    }
    if (x < width - 1) {
      neighbors :+= (coordToNodeId(x + 1, y), Port.East)
    }
    if (y > 0) {
      neighbors :+= (coordToNodeId(x, y - 1), Port.South)
    }
    if (y < height - 1) {
      neighbors :+= (coordToNodeId(x, y + 1), Port.North)
    }

    neighbors
  }

  override def connectRouters(routers: Seq[RouterIO]): Unit = {
    require(routers.length == numNodes, s"Expected ${numNodes} routers, got ${routers.length}")

    // Create channel connections
    val channels = collection.mutable.Map[(Int, Int), NoCChannel]()

    // Connect all adjacent routers
    for (nodeId <- 0 until numNodes) {
      val neighbors = getNeighbors(nodeId)

      for ((neighborId, port) <- neighbors) {
        val channelKey = if (nodeId < neighborId) (nodeId, neighborId) else (neighborId, nodeId)

        if (!channels.contains(channelKey)) {
          val channel = Module(new PipelineChannel(config))
          channels(channelKey) = channel

          // Determine connection direction
          if (nodeId < neighborId) {
            // nodeId -> neighborId
            val (srcPort, dstPort) = getConnection(nodeId, neighborId) match {
              case Some(p) => (p, Port.opposite(p))
              case None => throw new IllegalArgumentException("Invalid connection")
            }

            channel.io.in <> routers(nodeId).outPorts(srcPort.id)
            routers(neighborId).inPorts(dstPort.id) <> channel.io.out
          } else {
            // neighborId -> nodeId
            val (srcPort, dstPort) = getConnection(neighborId, nodeId) match {
              case Some(p) => (p, Port.opposite(p))
              case None => throw new IllegalArgumentException("Invalid connection")
            }

            channel.io.in <> routers(neighborId).outPorts(srcPort.id)
            routers(nodeId).inPorts(dstPort.id) <> channel.io.out
          }
        } else {
          // Other end of bidirectional connection
          val channel = channels(channelKey)
          val (srcPort, dstPort) = getConnection(nodeId, neighborId) match {
            case Some(p) => (p, Port.opposite(p))
            case None => throw new IllegalArgumentException("Invalid connection")
          }

          if (nodeId < neighborId) {
            channel.io.in <> routers(nodeId).outPorts(srcPort.id)
            routers(neighborId).inPorts(dstPort.id) <> channel.io.out
          } else {
            channel.io.in <> routers(neighborId).outPorts(srcPort.id)
            routers(nodeId).inPorts(dstPort.id) <> channel.io.out
          }
        }
      }
    }
  }
}
