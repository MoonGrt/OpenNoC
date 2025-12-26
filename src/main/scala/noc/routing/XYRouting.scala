package noc.routing

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}

/**
 * XYRouting - XY routing algorithm
 * Routes first along X direction, then along Y direction
 * Suitable for 2D Mesh topology
 *
 * @param config NoC configuration
 * @param meshWidth Mesh width (number of nodes in X direction)
 * @param meshHeight Mesh height (number of nodes in Y direction)
 */
class XYRouting(config: NoCConfig, meshWidth: Int, meshHeight: Int) extends DeterministicRouting(config) {
  require(meshWidth > 0 && meshHeight > 0, "Mesh dimensions must be positive")
  require(config.numPorts >= 4, "XY routing requires at least 4 ports (North, South, East, West)")

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
    val possiblePorts = Wire(Vec(config.totalPorts, Bool()))

    val currentX = getX(currentId)
    val currentY = getY(currentId)
    val destX = getX(destId)
    val destY = getY(destId)

    val xDiff = destX - currentX
    val yDiff = destY - currentY

    // Initialize all ports to false
    for (i <- 0 until config.totalPorts) {
      possiblePorts(i) := false.B
    }

    // XY routing: X first, then Y
    when(xDiff === 0.S.asUInt) {
      // Same X coordinate, route along Y direction
      when(yDiff === 0.S.asUInt) {
        // Reached destination, use Local port
        possiblePorts(Port.Local.id) := true.B
      }.elsewhen(yDiff.asSInt > 0.S) {
        // Need to go north
        possiblePorts(Port.North.id) := true.B
      }.otherwise {
        // Need to go south
        possiblePorts(Port.South.id) := true.B
      }
    }.otherwise {
      // Different X coordinate, route along X direction first
      when(xDiff.asSInt > 0.S) {
        // Need to go east
        possiblePorts(Port.East.id) := true.B
      }.otherwise {
        // Need to go west
        possiblePorts(Port.West.id) := true.B
      }
    }

    possiblePorts
  }
}
