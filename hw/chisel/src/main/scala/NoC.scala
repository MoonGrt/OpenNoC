package noc.system

import chisel3._
import chisel3.util._
import noc.config.NoCConfig
import noc.router.{Router, RouterIO}
import noc.ni.NetworkInterface
import noc.topology.NoCTopology

/**
 * NoC - Abstract base class for NoC systems
 * Integrates routers, network interfaces and topology structures to form a complete NoC system
 */
abstract class NoC(val config: NoCConfig) extends Module {
  /**
   * Get network interface list
   */
  def getNetworkInterfaces: Seq[NetworkInterface]

  /**
   * Get router list
   */
  def getRouters: Seq[Router]

  /**
   * Get topology structure
   */
  def getTopology: NoCTopology
}
