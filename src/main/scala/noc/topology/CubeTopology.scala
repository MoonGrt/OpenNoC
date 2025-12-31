package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{BiNoCChannel, BiPipelineChannel}

/**
 * CubeTopology - 3D Cube topology
 * Nodes arranged in a 3D grid, each node connected to six neighbors (up, down, left, right, front, back)
 *
 * @param config NoC configuration
 * @param width Mesh width (number of nodes in X direction)
 * @param height Mesh height (number of nodes in Y direction)
 * @param depth Mesh depth (number of nodes in Z direction)
 */
class CubeTopology(config: NoCConfig, val width: Int, val height: Int, val depth: Int) extends NoCTopology(config) {
  require(width > 0 && height > 0 && depth > 0, "Cube dimensions must be positive")
  require(config.portNum >= 6, "Cube topology requires at least 6 ports")

  override def numNodes: Int = width * height * depth

  /**
   * Convert node ID to coordinates (x, y, z)
   */
  private def nodeIdToCoord(nodeId: Int): (Int, Int, Int) = {
    val x = nodeId % width
    val y = (nodeId / width) % height
    val z = nodeId / (width * height)
    (x, y, z)
  }

  /**
   * Convert coordinates (x, y, z) to node ID
   */
  private def coordToNodeId(x: Int, y: Int, z: Int): Int = {
    z * width * height + y * width + x
  }

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.port] = {
    val (srcX, srcY, srcZ) = nodeIdToCoord(srcNodeId)
    val (dstX, dstY, dstZ) = nodeIdToCoord(dstNodeId)

    val dx = dstX - srcX
    val dy = dstY - srcY
    val dz = dstZ - srcZ

    if (dx == 1 && dy == 0 && dz == 0) {
      Some(Port.East)
    } else if (dx == -1 && dy == 0 && dz == 0) {
      Some(Port.West)
    } else if (dx == 0 && dy == 1 && dz == 0) {
      Some(Port.North)
    } else if (dx == 0 && dy == -1 && dz == 0) {
      Some(Port.South)
    } else if (dx == 0 && dy == 0 && dz == 1) {
      Some(Port.Up)
    } else if (dx == 0 && dy == 0 && dz == -1) {
      Some(Port.Down)
    } else {
      None
    }
  }

  override def getNeighbors(nodeId: Int): Seq[(Int, Port.port)] = {
    val (x, y, z) = nodeIdToCoord(nodeId)
    var neighbors = Seq.empty[(Int, Port.port)]

    if (x > 0) {
      neighbors :+= (coordToNodeId(x - 1, y, z), Port.West)
    }
    if (x < width - 1) {
      neighbors :+= (coordToNodeId(x + 1, y, z), Port.East)
    }
    if (y > 0) {
      neighbors :+= (coordToNodeId(x, y - 1, z), Port.South)
    }
    if (y < height - 1) {
      neighbors :+= (coordToNodeId(x, y + 1, z), Port.North)
    }
    if (z > 0) {
      neighbors :+= (coordToNodeId(x, y, z - 1), Port.Down)
    }
    if (z < depth - 1) {
      neighbors :+= (coordToNodeId(x, y, z + 1), Port.Up)
    }

    neighbors
  }

  override def connectRouters(routers: Seq[RouterIO]): Unit = {
    require(routers.length == numNodes, s"Expected ${numNodes} routers, got ${routers.length}")

    /*************************************************
     * 1. Default: Disable all Cube direction ports (for edge routers)
     *************************************************/
    for (r <- routers) {
      for (p <- Port.dirEWNSUD) { // East/West/North/South/Up/Down
        // inPorts: Without upstream, never valid
        r.inPorts(p.id).valid := false.B
        r.inPorts(p.id).bits  := 0.U.asTypeOf(r.inPorts(p.id).bits)
        // outPorts: No downstream, ready is always false
        r.outPorts(p.id).ready := false.B
      }
    }

    /*************************************************
     * 2. Establish a channel connection between adjacent routers in the cube network.
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
