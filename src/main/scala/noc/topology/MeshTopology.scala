package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{BiNoCChannel, BiPipelineChannel}

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

    /*************************************************
     * 1. Default: Disable all Mesh direction ports (for edge routers)
     *************************************************/
    for (r <- routers) {
      for (p <- Port.directions) { // East/West/North/South
        // inPorts: Without upstream, never valid
        r.inPorts(p.id).valid := false.B
        r.inPorts(p.id).bits  := 0.U.asTypeOf(r.inPorts(p.id).bits)
        // outPorts: No downstream, ready is always false
        r.outPorts(p.id).ready := false.B
      }
    }

    /*************************************************
     * 2. Establish a channel connection between adjacent routers in the mesh network.
     *************************************************/
    // Create channel connections
    val channels = collection.mutable.Map[(Int, Int), BiNoCChannel]()

    // Connect all adjacent routers
    for (nodeId <- 0 until numNodes) {
      val neighbors = getNeighbors(nodeId)

      for ((neighborId, _) <- neighbors) {
        // Undirected physical edge key
        val channelKey =
          if (nodeId < neighborId) (nodeId, neighborId)
          else (neighborId, nodeId)

        // If this edge has already been processed, skip it directly.
        if (!channels.contains(channelKey)) {
          val channel = Module(new BiPipelineChannel(config))
          channels(channelKey) = channel
          val (a, b) = channelKey
          // a -> b
          val (aOut, bIn) =
            getConnection(a, b) match {
              case Some(p) => (p, Port.opposite(p))
              case None    => throw new IllegalArgumentException("Invalid connection")
            }
          // b -> a
          val (bOut, aIn) =
            getConnection(b, a) match {
              case Some(p) => (p, Port.opposite(p))
              case None    => throw new IllegalArgumentException("Invalid connection")
            }

          // ===== Bidirectional connection =====
          // a → b
          routers(a).outPorts(aIn.id) <> channel.io.rx.in
          channel.io.rx.out <> routers(b).inPorts(bOut.id)
          // b → a
          routers(b).outPorts(bIn.id) <> channel.io.tx.in
          channel.io.tx.out <> routers(a).inPorts(aOut.id)
        }
      }
    }
  }
}
