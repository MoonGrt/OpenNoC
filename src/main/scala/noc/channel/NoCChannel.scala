package noc.channel

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * NoCChannel - Base interface for NoC channels
 * Defines the channel interface for connecting two components in NoC
 */
class NoCChannelIO(val config: NoCConfig) extends Bundle {
  val in = Flipped(Decoupled(new Flit(config)))
  val out = Decoupled(new Flit(config))
}

/**
 * NoCChannel - Abstract base class for channels
 * All channel implementations should extend this class
 */
abstract class NoCChannel(val config: NoCConfig) extends Module {
  val io = IO(new NoCChannelIO(config))
}
