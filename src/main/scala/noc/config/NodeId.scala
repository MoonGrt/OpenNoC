package noc.config

import chisel3._

/**
 * NodeId - Abstract representation of node ID
 * Used to identify each node (router or network interface) in the NoC
 */
object NodeId {
  def apply(id: Int, width: Int): UInt = id.U(width.W)

  def fromUInt(id: UInt): Int = id.litValue.toInt
}

/**
 * NodeIdConfig - Node ID configuration
 * @param nodeIdWidth Bit width of the node ID
 */
case class NodeIdConfig(nodeIdWidth: Int) {
  require(nodeIdWidth > 0, "NodeId width must be positive")

  def maxNodes: Int = (1 << nodeIdWidth) - 1
}
