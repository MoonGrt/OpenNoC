package noc.config

import chisel3._

/**
 * Port - Port direction enumeration
 * Defines commonly used port directions in NoC
 */
object Port extends Enumeration {
  type Port = Value

  val Local = Value(0)  // Local port (connected to network interface)
  val East  = Value(1)  // East
  val West  = Value(2)  // West
  val North = Value(3)  // North
  val South = Value(4)  // South

  // For 3D topologies
  val Up    = Value(5)  // Up
  val Down  = Value(6)  // Down

  /**
   * Get all directional ports (excluding Local)
   */
  def directions: Seq[Port] = Seq(North, South, East, West)

  /**
   * Get 3D directional ports
   */
  def directions3D: Seq[Port] = Seq(North, South, East, West, Up, Down)

  /**
   * Get opposite direction
   */
  def opposite(port: Port): Port = port match {
    case North => South
    case South => North
    case East  => West
    case West  => East
    case Up    => Down
    case Down  => Up
    case Local => Local
  }

  /**
   * Number of ports (excluding Local)
   */
  def numDirections: Int = directions.length

  /**
   * Number of 3D ports (excluding Local)
   */
  def numDirections3D: Int = directions3D.length
}

/**
 * PortConfig - Port configuration
 * @param numPorts Number of ports (excluding Local port)
 */
case class PortConfig(numPorts: Int) {
  require(numPorts > 0, "Number of ports must be positive")

  def totalPorts: Int = numPorts + 1  // Including Local port

  def portWidth: Int = chisel3.util.log2Ceil(totalPorts)
}
