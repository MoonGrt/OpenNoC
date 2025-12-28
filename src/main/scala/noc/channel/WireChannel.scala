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
class UniWireChannel(config: NoCConfig) extends UniNoCChannel(config) {
  io.out <> io.in
}
class BiWireChannel(config: NoCConfig) extends BiNoCChannel(config) {
  io.tx.out <> io.tx.in
  io.rx.out <> io.rx.in
}
