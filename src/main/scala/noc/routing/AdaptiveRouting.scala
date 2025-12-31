package noc.routing

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}

/**
 * AdaptiveRouting - Abstract base class for adaptive routing
 * Dynamically selects paths based on network state, can avoid congestion
 */
abstract class AdaptiveRouting(config: NoCConfig) extends RoutingPolicy(config) {
  /**
   * Select port based on congestion information
   * @param possiblePorts Possible ports
   * @param congestionInfo Congestion information (congestion level for each port, higher value means more congested)
   */
  protected def selectLeastCongestedPort(possiblePorts: Vec[Bool], congestionInfo: Vec[UInt]): UInt = {
    val portWidth = chisel3.util.log2Ceil(config.portNum)
    val selected = Wire(UInt(portWidth.W))

    // Find the available port with lowest congestion
    var minCongestion = Wire(UInt(congestionInfo(0).getWidth.W))
    var minPort = Wire(UInt(portWidth.W))

    minCongestion := congestionInfo(0)
    minPort := 0.U

    for (i <- 1 until config.portNum) {
      when(possiblePorts(i) && congestionInfo(i) < minCongestion) {
        minCongestion := congestionInfo(i)
        minPort := i.U
      }
    }

    selected := minPort
    selected
  }
}

/**
 * AdaptiveXYRouting - Adaptive XY routing
 * Based on XY routing, adaptively selects paths based on congestion conditions
 *
 * @param config NoC configuration
 * @param meshWidth Mesh width
 * @param meshHeight Mesh height
 */
class AdaptiveXYRouting(config: NoCConfig, meshWidth: Int, meshHeight: Int) extends AdaptiveRouting(config) {
  require(meshWidth > 0 && meshHeight > 0, "Mesh dimensions must be positive")
  require(config.portNum >= 4, "Adaptive XY routing requires at least 4 ports")

  /**
   * Extract X coordinate from node ID
   */
  private def getX(nodeId: UInt): UInt = {
    nodeId % meshWidth.U
  }

  /**
   * Extract Y coordinate from node ID
   */
  private def getY(nodeId: UInt): UInt = {
    nodeId / meshWidth.U
  }

  override def getPossiblePorts(currentId: UInt, destId: UInt): Vec[Bool] = {
    val possiblePorts = Wire(Vec(config.portNum, Bool()))

    val currentX = getX(currentId)
    val currentY = getY(currentId)
    val destX = getX(destId)
    val destY = getY(destId)

    val xDiff = destX - currentX
    val yDiff = destY - currentY

    // Initialize all ports to false
    for (i <- 0 until config.portNum) {
      possiblePorts(i) := false.B
    }

    // Adaptive XY routing: allows selecting any direction when both X and Y directions are available
    when(xDiff === 0.S.asUInt && yDiff === 0.S.asUInt) {
      // Reached destination
      possiblePorts(Port.Local.id) := true.B
    }.elsewhen(xDiff === 0.S.asUInt) {
      // Same X coordinate, can only route along Y direction
      when(yDiff.asSInt > 0.S) {
        possiblePorts(Port.North.id) := true.B
      }.otherwise {
        possiblePorts(Port.South.id) := true.B
      }
    }.elsewhen(yDiff === 0.S.asUInt) {
      // Same Y coordinate, can only route along X direction
      when(xDiff.asSInt > 0.S) {
        possiblePorts(Port.East.id) := true.B
      }.otherwise {
        possiblePorts(Port.West.id) := true.B
      }
    }.otherwise {
      // Both X and Y are different, can choose X or Y direction (adaptive)
      when(xDiff.asSInt > 0.S) {
        possiblePorts(Port.East.id) := true.B
      }.otherwise {
        possiblePorts(Port.West.id) := true.B
      }
      when(yDiff.asSInt > 0.S) {
        possiblePorts(Port.North.id) := true.B
      }.otherwise {
        possiblePorts(Port.South.id) := true.B
      }
    }

    possiblePorts
  }

  override def route(currentId: UInt, destId: UInt): UInt = {
    val possiblePorts = getPossiblePorts(currentId, destId)
    // Simplified version: uses priority encoder (should use congestion info in practice)
    // Full implementation requires congestionInfo parameter
    val portWidth = chisel3.util.log2Ceil(config.portNum)
    val selected = PriorityEncoder(possiblePorts.asUInt)
    selected
  }

  /**
   * Routing decision with congestion information
   */
  def routeWithCongestion(currentId: UInt, destId: UInt, congestionInfo: Vec[UInt]): UInt = {
    val possiblePorts = getPossiblePorts(currentId, destId)
    selectLeastCongestedPort(possiblePorts, congestionInfo)
  }
}
