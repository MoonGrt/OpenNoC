package noc.router

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig
import noc.channel.UniBufferedChannel
import noc.arbiter.{NoCArbiter, RoundRobin}

/**
 * VirtualChannel - Virtual channel implementation
 *
 * @param config NoC configuration
 */
class VirtualChannel(config: NoCConfig) extends Module {
  require(config.bufferDepth > 0, "VirtualChannel buffer depth must be positive")

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new Flit(config)))
    val out = Decoupled(new Flit(config))
  })

  // Create virtual channels
  val vc = Seq.fill(config.vcNum) {
    Module(new UniBufferedChannel(config, config.bufferDepth))
  }

  // Create arbiter for output port
  val arbiters = Module(new RoundRobin(config, config.vcNum))

  // VC wiring
  for (i <- 0 until config.vcNum) {
    // Connect input to virtual channel
    val selected = io.in.bits.vcId  === i.U
    vc(i).io.in.bits := io.in.bits
    vc(i).io.in.valid := io.in.valid && selected
    io.in.ready := Mux(selected, RegNext(vc(i).io.in.ready), false.B)
    // Connect output from virtual channel to arbiter
    arbiters.io.in(i) <> vc(i).io.out
  }
  io.out <> arbiters.io.out
}
