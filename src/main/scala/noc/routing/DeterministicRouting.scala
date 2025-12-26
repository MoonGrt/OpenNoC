package noc.routing

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}

/**
 * DeterministicRouting - Abstract base class for deterministic routing
 * Deterministic routing algorithms always select the same path for the same source-destination pair
 */
abstract class DeterministicRouting(config: NoCConfig) extends RoutingPolicy(config) {
  /**
   * Select one port from possible ports (default selects the first available)
   */
  protected def selectPort(possiblePorts: Vec[Bool]): UInt = {
    val portWidth = chisel3.util.log2Ceil(config.totalPorts)
    val selected = Wire(UInt(portWidth.W))

    // Priority encoder: select the first port that is true
    val priorityEncoder = PriorityEncoder(possiblePorts.asUInt)
    selected := priorityEncoder

    selected
  }

  override def route(currentId: UInt, destId: UInt, availablePorts: Vec[Bool]): UInt = {
    val possiblePorts = getPossiblePorts(currentId, destId)
    // Only consider ports that are both possible and available
    val validPorts = VecInit(possiblePorts.zip(availablePorts).map { case (p, a) => p && a })
    selectPort(validPorts)
  }
}
