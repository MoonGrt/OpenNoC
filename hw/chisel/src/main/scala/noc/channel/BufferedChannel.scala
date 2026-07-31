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
class UniBufferedChannel(config: NoCConfig, depth: Int = 4) extends UniNoCChannel(config) {
  require(depth > 0, "Buffer depth must be positive")
  import config._

  val queue = Module(new Queue(new Flit(flitConfig), depth, pipe = false, flow = false))
  queue.io.enq <> io.in
  queue.io.deq <> io.out
}
class BiBufferedChannel(config: NoCConfig, depth: Int = 4) extends BiNoCChannel(config) {
  require(depth > 0, "Buffer depth must be positive")
  import config._

  // tx
  val queue1 = Module(new Queue(new Flit(flitConfig), depth, pipe = false, flow = false))
  queue1.io.enq <> io.tx.in
  queue1.io.deq <> io.tx.out
  // rx
  val queue2 = Module(new Queue(new Flit(flitConfig), depth, pipe = false, flow = false))
  queue2.io.enq <> io.rx.in
  queue2.io.deq <> io.rx.out
}
