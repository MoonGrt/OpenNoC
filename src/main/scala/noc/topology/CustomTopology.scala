package noc.topology

import chisel3._
import noc.config.NoCConfig
import noc.router.RouterIO

/**
 * CustomTopology - Custom topology
 * Allows users to define arbitrary topology structures
 *
 * @param config NoC configuration
 * @param connections Connection list, each element is (source node ID, destination node ID, source port, destination port)
 */
class CustomTopology(
  config: NoCConfig,
  override val numNodes: Int,
  connections: Seq[(Int, Int, Int, Int)]
) extends NoCTopology(config) {

  require(numNodes > 0, "Number of nodes must be positive")

  // Build connection map
  private val connectionMap = connections.groupBy(_._1).mapValues(_.map(t => (t._2, t._3, t._4)))

  override def getConnection(srcNodeId: Int, dstNodeId: Int): Option[noc.config.Port.Port] = {
    connectionMap.get(srcNodeId).flatMap { conns =>
      conns.find(_._1 == dstNodeId).map { case (_, srcPort, _) =>
        noc.config.Port(srcPort)
      }
    }
  }

  override def getNeighbors(nodeId: Int): Seq[(Int, noc.config.Port.Port)] = {
    connectionMap.getOrElse(nodeId, Seq.empty).map { case (dstId, srcPort, _) =>
      (dstId, noc.config.Port(srcPort))
    }
  }

  override def connectRouters(routers: Seq[RouterIO]): Unit = {
    require(routers.length == numNodes, s"Expected ${numNodes} routers, got ${routers.length}")

    import noc.channel.PipelineChannel

    // Create channel for each connection pair
    val channelMap = collection.mutable.Map[(Int, Int), PipelineChannel]()

    for ((srcId, dstId, srcPort, dstPort) <- connections) {
      val key = (srcId, dstId)

      if (!channelMap.contains(key)) {
        val channel = Module(new PipelineChannel(config))
        channelMap(key) = channel

        channel.io.in <> routers(srcId).outPorts(srcPort)
        routers(dstId).inPorts(dstPort) <> channel.io.out
      }
    }
  }
}
