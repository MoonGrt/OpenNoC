package noc.ni

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * NetworkInterface - Abstract base class for network interfaces
 * Connects computing units to NoC network, responsible for packet packing and unpacking
 */
abstract class NetworkInterface(val config: NoCConfig, val nodeId: Int) extends Module {}
