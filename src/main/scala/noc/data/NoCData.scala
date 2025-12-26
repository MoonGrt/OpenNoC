package noc.data

import chisel3._
import chisel3.util._

/**
 * NoCData - Base trait for NoC data packets
 * Defines the basic interface for data transmission in NoC
 */
trait NoCData extends Bundle {
  def data: UInt
}
