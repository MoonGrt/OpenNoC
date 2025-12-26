package noc.routing

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}

/**
 * RoutingPolicy - Abstract base class for routing policies
 * Defines the interface for routing algorithms
 */
abstract class RoutingPolicy(val config: NoCConfig) {
  /**
   * Compute routing decision
   * @param currentId Current node ID
   * @param destId Destination node ID
   * @param availablePorts Available port list (Bool vector)
   * @return Selected output port index
   */
  def route(currentId: UInt, destId: UInt, availablePorts: Vec[Bool]): UInt

  /**
   * Get all possible output ports
   * @param currentId Current node ID
   * @param destId Destination node ID
   * @return Possible output port list (Bool vector, true means port is available)
   */
  def getPossiblePorts(currentId: UInt, destId: UInt): Vec[Bool]
}
