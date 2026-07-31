package noc.ni

import chisel3._
import chisel3.util._
import noc.config.NoCConfig
import noc.data.Flit

class PacketInputBeat(config: NoCConfig) extends Bundle {
  val data = UInt(config.dataWidth.W)
  val dest = UInt(config.nodeIdWidth.W)
  val last = Bool()
}

class PacketOutputBeat(config: NoCConfig) extends Bundle {
  val data = UInt(config.dataWidth.W)
  val src = UInt(config.nodeIdWidth.W)
  val dest = UInt(config.nodeIdWidth.W)
  val last = Bool()
}

/** Packet stream to wormhole-flit adapter.
  *
  * Destination is captured on the first accepted stream beat and all
  * subsequent beats use the captured value until `last`.
  */
class PacketStreamNI(config: NoCConfig, nodeId: Int, nodeCount: Int)
    extends NetworkInterface(config, nodeId) {
  val io = IO(new Bundle {
    val routerLink = new Bundle {
      val out = Decoupled(new Flit(config.flitConfig))
      val in = Flipped(Decoupled(new Flit(config.flitConfig)))
    }
    val streamIn = Flipped(Decoupled(new PacketInputBeat(config)))
    val streamOut = Decoupled(new PacketOutputBeat(config))
  })

  val sendQueue = Module(new Queue(new PacketInputBeat(config), config.bufferDepth))
  val sendActive = RegInit(false.B)
  val sendDest = Reg(UInt(config.nodeIdWidth.W))
  val effectiveDest = Mux(sendActive, sendDest, io.streamIn.bits.dest)

  val legalDest = effectiveDest < nodeCount.U
  sendQueue.io.enq.valid := io.streamIn.valid && legalDest
  sendQueue.io.enq.bits.data := io.streamIn.bits.data
  sendQueue.io.enq.bits.dest := effectiveDest
  sendQueue.io.enq.bits.last := io.streamIn.bits.last
  io.streamIn.ready := sendQueue.io.enq.ready && legalDest
  when(sendQueue.io.enq.fire) {
    when(!sendActive && !io.streamIn.bits.last) {
      sendActive := true.B
      sendDest := io.streamIn.bits.dest
    }
    when(io.streamIn.bits.last) {
      sendActive := false.B
    }
  }

  val transmitting = RegInit(false.B)
  val beat = sendQueue.io.deq.bits
  io.routerLink.out.valid := sendQueue.io.deq.valid
  io.routerLink.out.bits := Mux(
    !transmitting,
    Mux(
      beat.last,
      Flit.headTail(config.flitConfig, 0.U, nodeId.U, beat.dest, beat.data),
      Flit.head(config.flitConfig, 0.U, nodeId.U, beat.dest, beat.data)
    ),
    Mux(
      beat.last,
      Flit.tail(config.flitConfig, 0.U, beat.data),
      Flit.body(config.flitConfig, 0.U, beat.data)
    )
  )
  sendQueue.io.deq.ready := io.routerLink.out.ready
  when(io.routerLink.out.fire) {
    transmitting := !beat.last
  }

  val recvSrc = Reg(UInt(config.nodeIdWidth.W))
  val recvDest = Reg(UInt(config.nodeIdWidth.W))
  val receiving = RegInit(false.B)
  val recvQueue = Module(new Queue(new PacketOutputBeat(config), config.bufferDepth))
  val incoming = io.routerLink.in.bits
  val currentSrc = Mux(incoming.isHead, incoming.srcId, recvSrc)
  val currentDest = Mux(incoming.isHead, incoming.dstId, recvDest)

  recvQueue.io.enq.valid := io.routerLink.in.valid
  recvQueue.io.enq.bits.data := incoming.data
  recvQueue.io.enq.bits.src := currentSrc
  recvQueue.io.enq.bits.dest := currentDest
  recvQueue.io.enq.bits.last := incoming.isTail
  io.routerLink.in.ready := recvQueue.io.enq.ready
  when(io.routerLink.in.fire) {
    when(incoming.isHead) {
      recvSrc := incoming.srcId
      recvDest := incoming.dstId
    }
    receiving := !incoming.isTail
  }
  assert(!io.routerLink.in.valid || receiving || incoming.isHead,
    "PacketStreamNI received body/tail without a head")

  io.streamOut <> recvQueue.io.deq
}
