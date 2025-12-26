package noc.channel

import chisel3._
import chisel3.util._
import noc.data.Flit
import noc.config.NoCConfig

/**
 * BufferedChannel - Buffered channel
 * Uses FIFO buffer to store multiple flits
 *
 * @param config NoC configuration
 * @param depth Buffer depth
 */
class BufferedChannel(config: NoCConfig, depth: Int = 4) extends NoCChannel(config) {
  require(depth > 0, "Buffer depth must be positive")

  val queue = Module(new Queue(new Flit(config), depth, pipe = false, flow = false))

  queue.io.enq <> io.in
  queue.io.deq <> io.out
}
