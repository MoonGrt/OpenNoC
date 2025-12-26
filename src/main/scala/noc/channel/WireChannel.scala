package noc.channel

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * WireChannel - Simple wire channel
 * Directly connects input to output, no delay, no buffering
 * Suitable for combinational logic connections
 */
class WireChannel(config: NoCConfig) extends NoCChannel(config) {
  io.out <> io.in
}
