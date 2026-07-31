package noc.topology

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO
import noc.channel.{BiPipelineChannel}

/**
 * RingTopology - Ring topology
 * Nodes arranged in a ring, each node connected to left and right neighbors
 *
 * @param config NoC configuration
 * @param numNodes Number of nodes
 */
class RingTopology(config: NoCConfig, override val numNodes: Int) extends NoCTopology(config) {
  require(numNodes > 0, "Number of nodes must be positive")
  require(config.portNum >= 2, "Ring topology requires at least 2 ports")

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.port] = {
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

  override def getNeighbors(nodeId: Int): Seq[(Int, Port.port)] = {
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
    val channels = Seq.fill(numNodes) {  // Create channels from input to output
      Module(new BiPipelineChannel(config))
    }

    // Connect into a ring
    for (i <- 0 until numNodes) {
      val next = (i + 1) % numNodes
      // i -> next (East direction)
      routers(i).outPorts(Port.East.id) <> channels(i).io.tx.in
      channels(i).io.tx.out <> routers(next).inPorts(Port.West.id)
      // next -> i (West direction)
      routers(next).outPorts(Port.West.id) <> channels(i).io.rx.in
      channels(i).io.rx.out <> routers(i).inPorts(Port.East.id)
    }
  }
}
