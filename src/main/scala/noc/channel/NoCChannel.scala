package noc.channel

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * Unidirectional Channel Bundle
 * 
 * @param config NoC configuration
 */
class UniChannel(val config: NoCConfig) extends Bundle {
  val in  = Flipped(Decoupled(new Flit(config)))
  val out = Decoupled(new Flit(config))
}

/**
 * Bidirectional Channel Bundle
 * 
 * @param config NoC configuration
 */
class BiChannel(val config: NoCConfig) extends Bundle {
  val tx = new UniChannel(config)
  val rx = new UniChannel(config)
}

/**
 * NoCChannel - Abstract base class for channels
 * All channel implementations should extend this class
 * 
 * @param config NoC configuration
 */
abstract class UniNoCChannel(val config: NoCConfig) extends Module {
  val io = IO(new UniChannel(config))
}
abstract class BiNoCChannel(val config: NoCConfig) extends Module {
  val io = IO(new BiChannel(config))
}
