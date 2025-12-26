package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{NoCChannel, PipelineChannel}

/**
 * RingTopology - Ring topology
 * Nodes arranged in a ring, each node connected to left and right neighbors
 *
 * @param config NoC configuration
 * @param numNodes Number of nodes
 */
class RingTopology(config: NoCConfig, override val numNodes: Int) extends NoCTopology(config) {
  require(numNodes > 0, "Number of nodes must be positive")
  require(config.numPorts >= 2, "Ring topology requires at least 2 ports")

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.Port] = {
    val diff = (dstNodeId - srcNodeId + numNodes) % numNodes

    if (diff == 1 || diff == numNodes - 1) {
      // Adjacent nodes
      if (diff == 1) {
        Some(Port.East)  // Clockwise direction
      } else {
        Some(Port.West)  // Counter-clockwise direction
      }
    } else {
      None
    }
  }

  override def getNeighbors(nodeId: Int): Seq[(Int, Port.Port)] = {
    val leftNeighbor = (nodeId - 1 + numNodes) % numNodes
    val rightNeighbor = (nodeId + 1) % numNodes

    Seq(
      (leftNeighbor, Port.West),
      (rightNeighbor, Port.East)
    )
  }

  override def connectRouters(routers: Seq[RouterIO]): Unit = {
    require(routers.length == numNodes, s"Expected ${numNodes} routers, got ${routers.length}")

    // Create channel connections
    val channels = Seq.fill(numNodes) {
      Module(new PipelineChannel(config))
    }

    // Connect into a ring
    for (i <- 0 until numNodes) {
      val next = (i + 1) % numNodes

      // i -> next (East direction)
      channels(i).io.in <> routers(i).outPorts(Port.East.id)
      routers(next).inPorts(Port.West.id) <> channels(i).io.out
    }
  }
}
