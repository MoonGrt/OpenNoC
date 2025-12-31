package noc.config

import chisel3._

/**
 * Port - Port direction enumeration
 * Defines commonly used port directions in NoC
 */
object Port extends Enumeration {
  type port = Value

  val Local = Value(0)  // Local port (connected to network interface)
  val East  = Value(1)  // East
  val West  = Value(2)  // West
  val North = Value(3)  // North
  val South = Value(4)  // South

  // For 3D topologies
  val Up    = Value(5)  // Up
  val Down  = Value(6)  // Down

  /**
   * East-West directions
   */
  def dirEW: Seq[port] = Seq(East, West)

  /**
   * East-West-North-South directions
   */
  def dirEWNS: Seq[port] = Seq(East, West, North, South)

  /**
   * East-West-North-South-Up-Down directions
   */
  def dirEWNSUD: Seq[port] = Seq(North, South, East, West, Up, Down)

  /**
   * Get opposite direction
   */
  def opposite(port: port): port = port match {
    case North => South
    case South => North
    case East  => West
    case West  => East
    case Up    => Down
    case Down  => Up
    case Local => Local
  }
}

/**
 * PortConfig - Port configuration
 * @param ports List of ports (excluding Local)
 */
case class PortConfig(ports: Seq[Port.port]) {
  require(ports.nonEmpty, "Port list must be non-empty")

  // Add Local Port to List
  val Ports: Seq[Port.port] = Port.Local +: ports

  def portNum: Int = Ports.length
  def portWidth: Int = chisel3.util.log2Ceil(portNum)

  // Determine if a Port Exists
  def hasPort(port: Port.port): Boolean = Ports.contains(port)
}
