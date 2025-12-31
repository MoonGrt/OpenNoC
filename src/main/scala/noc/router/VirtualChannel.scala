package noc.router

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig
import noc.channel.UniBufferedChannel
import noc.arbiter._

/**
 * VirtualChannel - Virtual channel implementation
 *
 * @param config NoC configuration
 */
class VirtualChannel(config: NoCConfig) extends Module {
  require(config.bufferDepth > 0, "VirtualChannel buffer depth must be positive")
  import config._

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new Flit(flitConfig)))
    val out = Decoupled(new Flit(flitConfig))
  })

  // Create virtual channels
  val vc = Seq.fill(routerConfig.vcConfig.vcNum) {
    Module(new UniBufferedChannel(config, routerConfig.vcConfig.bufferDepth))
  }

  // Create arbiter for output port
  val arbiter: NoCArbiter = routerConfig.vcConfig.arbiterType match {
    case "RoundRobin" => Module(new RoundRobin(config, routerConfig.vcConfig.vcNum))
    case "FixedPriority" => Module(new FixedPriority(config, routerConfig.vcConfig.vcNum))
    case _ => throw new IllegalArgumentException(s"Unknown arbiter type: ${routerConfig.vcConfig.arbiterType}")
  }

  // VC wiring
  for (i <- 0 until routerConfig.vcConfig.vcNum) {
    // Connect input to virtual channel
    val selected = io.in.bits.vcId === i.U
    vc(i).io.in.bits := io.in.bits
    vc(i).io.in.valid := io.in.valid && selected
    io.in.ready := Mux(selected, RegNext(vc(i).io.in.ready), false.B)
    // Connect output from virtual channel to arbiter
    arbiter.io.in(i) <> vc(i).io.out
  }
  io.out <> arbiter.io.out
}
