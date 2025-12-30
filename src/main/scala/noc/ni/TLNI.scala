package noc.ni

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * TLNI - TileLink network interface
 * Provides TileLink protocol interface
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class TLNI(config: NoCConfig, nodeId: Int) extends NetworkInterface(config, nodeId) {
  import config._

  val io = IO(new Bundle {
    // Inherit base interface
    val routerLink = new Bundle {
      val out = Decoupled(new Flit(flitConfig))
      val in = Flipped(Decoupled(new Flit(flitConfig)))
    }
    val nodeId = Output(UInt(config.nodeIdWidth.W))

    // TileLink interface (simplified)
    val a = Flipped(Decoupled(new Bundle {
      val opcode = UInt(3.W)
      val param = UInt(3.W)
      val size = UInt(3.W)
      val source = UInt(8.W)
      val address = UInt(32.W)
      val data = UInt(config.dataWidth.W)
      val mask = UInt((config.dataWidth / 8).W)
    }))
    val d = Decoupled(new Bundle {
      val opcode = UInt(3.W)
      val param = UInt(3.W)
      val size = UInt(3.W)
      val source = UInt(8.W)
      val data = UInt(config.dataWidth.W)
    })
  })

  io.nodeId := nodeId.U(config.nodeIdWidth.W)

  // Transmit channel: pack TileLink requests into flits
  val sendQueue = Module(new Queue(io.a.bits.cloneType, config.bufferDepth))
  sendQueue.io.enq <> io.a

  val sendState = RegInit(0.U(2.W))

  io.routerLink.out.valid := false.B
  io.routerLink.out.bits := DontCare
  sendQueue.io.deq.ready := false.B

  when(sendQueue.io.deq.valid) {
    val req = sendQueue.io.deq.bits
    val flitData = Cat(
      req.mask,
      req.data,
      req.address,
      req.source,
      req.size,
      req.param,
      req.opcode
    )
    io.routerLink.out.valid := true.B
    io.routerLink.out.bits := Flit.headTail(flitConfig, nodeId.U, req.address(31, 24), flitData)
    sendQueue.io.deq.ready := io.routerLink.out.ready
  }

  // Receive channel: unpack flits into TileLink responses
  val recvQueue = Module(new Queue(io.d.bits.cloneType, config.bufferDepth))

  io.routerLink.in.ready := false.B
  recvQueue.io.enq.valid := false.B
  recvQueue.io.enq.bits := DontCare

  when(io.routerLink.in.valid) {
    val flit = io.routerLink.in.bits
    val resp = Wire(io.d.bits.cloneType)
    resp.opcode := flit.data(2, 0)
    resp.param := flit.data(5, 3)
    resp.size := flit.data(8, 6)
    resp.source := flit.data(16, 9)
    resp.data := flit.data(48, 17)

    recvQueue.io.enq.valid := true.B
    recvQueue.io.enq.bits := resp
    io.routerLink.in.ready := recvQueue.io.enq.ready
  }

  io.d <> recvQueue.io.deq
}
