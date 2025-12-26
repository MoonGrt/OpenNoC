package demo

import chisel3._
import chisel3.util.Decoupled
import noc.config.NoCConfig
import noc.channel.{WireChannel, PipelineChannel, BufferedChannel}
import noc.data.Flit

/**
 * ChannelExample - Example of using different channel types
 *
 * This example demonstrates how to:
 * 1. Use WireChannel for combinational connections
 * 2. Use PipelineChannel for pipelined connections
 * 3. Use BufferedChannel for buffered connections
 */
class ChannelExample extends Module {
  val config = NoCConfig(
    dataWidth = 32,
    flitWidth = 32,
    vcNum = 1,
    bufferDepth = 4,
    nodeIdWidth = 8,
    numPorts = 4
  )

  // Example 1: WireChannel - no delay, combinational
  val wireChannel = Module(new WireChannel(config))

  // Example 2: PipelineChannel - one cycle delay
  val pipelineChannel = Module(new PipelineChannel(config))

  // Example 3: BufferedChannel - FIFO buffer with depth 8
  val bufferedChannel = Module(new BufferedChannel(config, depth = 8))

  // Connect channels in series
  wireChannel.io.in <> pipelineChannel.io.out
  pipelineChannel.io.in <> bufferedChannel.io.out

  // Example: Connect input to buffered channel
  val inputFlit = Wire(Decoupled(new Flit(config)))
  bufferedChannel.io.in <> inputFlit

  // Example: Connect output from wire channel
  val outputFlit = Wire(Decoupled(new Flit(config)))
  outputFlit <> wireChannel.io.out
}
