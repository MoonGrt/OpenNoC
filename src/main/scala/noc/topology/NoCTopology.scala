package noc.topology

import chisel3._
import noc.config.{NoCConfig, Port}
import noc.router.RouterIO

/**
 * NoCTopology - Abstract base class for NoC topologies
 * Defines the topology interface for connecting routers and network interfaces
 */
abstract class NoCTopology(val config: NoCConfig) {
  /**
   * Get total number of nodes
   */
  def numNodes: Int

  /**
   * Get connection relationship between two nodes
   * @param srcNodeId Source node ID
   * @param dstNodeId Destination node ID
   * @return If direct connection exists, return Some(port direction), otherwise None
   */
  def getConnection(srcNodeId: Int, dstNodeId: Int): Option[Port.Port]

  /**
   * Get all neighbors of a node
   * @param nodeId Node ID
   * @return List of neighbor node IDs (including port direction)
   */
  def getNeighbors(nodeId: Int): Seq[(Int, Port.Port)]

  /**
   * Connect routers
   * Connect all routers according to topology structure
   * @param routers List of router IOs
   */
  def connectRouters(routers: Seq[RouterIO]): Unit
}
