package noc.routing

import chisel3._
import chisel3.util._
import noc.config.NoCConfig

/**
 * RoutingPolicy - Abstract base class for routing policies
 * Defines the interface for routing algorithms
 */
abstract class RoutingPolicy(val config: NoCConfig) {
  /**
   * Compute routing decision
   * @param currentId Current node ID
   * @param destId Destination node ID
   * @return Selected output port index
   */
  def route(currentId: UInt, destId: UInt): UInt

  /**
   * Get all possible output ports
   * @param currentId Current node ID
   * @param destId Destination node ID
   * @return Possible output port list (Bool vector, true means port is available)
   */
  def getPossiblePorts(currentId: UInt, destId: UInt): Vec[Bool]
}
