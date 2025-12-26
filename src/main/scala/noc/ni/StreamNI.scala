package noc.ni

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * StreamNI - Stream network interface
 * Provides simple stream interface, packs data stream into flits for transmission
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class StreamNI(config: NoCConfig, nodeId: Int) extends NetworkInterface(config, nodeId) {
  val io = IO(new Bundle {
    // Inherit base interface
    val routerLink = new Bundle {
      val out = Decoupled(new Flit(config))
      val in = Flipped(Decoupled(new Flit(config)))
    }
    val nodeId = Output(UInt(config.nodeIdWidth.W))

    // Stream interface
    val streamIn = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val streamOut = Decoupled(UInt(config.dataWidth.W))
    val destId = Input(UInt(config.nodeIdWidth.W))  // Destination node ID
  })

  io.nodeId := nodeId.U(config.nodeIdWidth.W)

  // Transmit side: pack data stream into flits
  val sendQueue = Module(new Queue(UInt(config.dataWidth.W), config.bufferDepth))
  sendQueue.io.enq <> io.streamIn

  val sendState = RegInit(0.U(2.W))  // 0: idle, 1: sending head, 2: sending body/tail
  val sendCounter = RegInit(0.U(8.W))
  val sendDestId = Reg(UInt(config.nodeIdWidth.W))

  io.routerLink.out.valid := false.B
  io.routerLink.out.bits := DontCare
  sendQueue.io.deq.ready := false.B

  switch(sendState) {
    is(0.U) {
      // Wait for data
      when(sendQueue.io.deq.valid) {
        sendState := 1.U
        sendDestId := io.destId
        sendCounter := 1.U
        // Send head flit
        io.routerLink.out.valid := true.B
        io.routerLink.out.bits := Flit.head(config, nodeId.U, io.destId, sendQueue.io.deq.bits)
        sendQueue.io.deq.ready := io.routerLink.out.ready
      }
    }
    is(1.U) {
      // Send body flit or tail flit
      when(sendQueue.io.deq.valid) {
        val isLast = sendCounter === (config.bufferDepth - 1).U || !sendQueue.io.deq.valid
        io.routerLink.out.valid := true.B
        when(isLast) {
          io.routerLink.out.bits := Flit.tail(config, sendQueue.io.deq.bits)
          sendState := 0.U
        }.otherwise {
          io.routerLink.out.bits := Flit.body(config, sendQueue.io.deq.bits)
        }
        sendQueue.io.deq.ready := io.routerLink.out.ready
        sendCounter := sendCounter + 1.U
      }
    }
  }

  // Receive side: unpack flits into data stream
  val recvQueue = Module(new Queue(UInt(config.dataWidth.W), config.bufferDepth))
  val recvState = RegInit(0.U(2.W))  // 0: waiting head, 1: receiving

  io.routerLink.in.ready := false.B
  recvQueue.io.enq.valid := false.B
  recvQueue.io.enq.bits := DontCare

  switch(recvState) {
    is(0.U) {
      // Wait for head flit
      when(io.routerLink.in.valid && io.routerLink.in.bits.isHead) {
        recvQueue.io.enq.valid := true.B
        recvQueue.io.enq.bits := io.routerLink.in.bits.data
        io.routerLink.in.ready := recvQueue.io.enq.ready
        when(io.routerLink.in.bits.isTail) {
          // Single-flit packet
          recvState := 0.U
        }.otherwise {
          recvState := 1.U
        }
      }
    }
    is(1.U) {
      // Receive body flit or tail flit
      when(io.routerLink.in.valid) {
        recvQueue.io.enq.valid := true.B
        recvQueue.io.enq.bits := io.routerLink.in.bits.data
        io.routerLink.in.ready := recvQueue.io.enq.ready
        when(io.routerLink.in.bits.isTail) {
          recvState := 0.U
        }
      }
    }
  }

  io.streamOut <> recvQueue.io.deq
}
