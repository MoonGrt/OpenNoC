package noc.ni

import chisel3._
import chisel3.util._
import noc.data.{Flit, Packet}
import noc.config.NoCConfig

/**
 * AXINI - AXI network interface
 * Provides AXI-like interface, supports address and data separation
 *
 * @param config NoC configuration
 * @param nodeId Node ID
 */
class AXINI(config: NoCConfig, nodeId: Int) extends NetworkInterface(config, nodeId) {
  import config._

  val io = IO(new Bundle {
    // Inherit base interface
    val routerLink = new Bundle {
      val out = Decoupled(new Flit(flitConfig))
      val in = Flipped(Decoupled(new Flit(flitConfig)))
    }
    val nodeId = Output(UInt(config.nodeIdWidth.W))

    // AXI-like interface
    val aw = Flipped(Decoupled(new Bundle {
      val addr = UInt(config.dataWidth.W)
      val destId = UInt(config.nodeIdWidth.W)
    }))
    val w = Flipped(Decoupled(UInt(config.dataWidth.W)))
    val b = Decoupled(Bool())  // Write response

    val ar = Flipped(Decoupled(new Bundle {
      val addr = UInt(config.dataWidth.W)
      val destId = UInt(config.nodeIdWidth.W)
    }))
    val r = Decoupled(new Bundle {
      val data = UInt(config.dataWidth.W)
      val last = Bool()
    })
  })

  io.nodeId := nodeId.U(config.nodeIdWidth.W)

  // Write channel: pack address and data into flit for transmission
  val writeState = RegInit(0.U(2.W))
  val writeDestId = Reg(UInt(config.nodeIdWidth.W))

  io.aw.ready := false.B
  io.w.ready := false.B
  io.b.valid := false.B
  io.b.bits := true.B
  io.routerLink.out.valid := false.B
  io.routerLink.out.bits := DontCare

  switch(writeState) {
    is(0.U) {
      // Wait for address
      when(io.aw.valid) {
        writeDestId := io.aw.bits.destId
        writeState := 1.U
        io.aw.ready := true.B
      }
    }
    is(1.U) {
      // Wait for data and send
      when(io.w.valid) {
        val flitData = Cat(io.w.bits, io.aw.bits.addr)  // Simplified: combine address and data
        io.routerLink.out.valid := true.B
        io.routerLink.out.bits := Flit.headTail(flitConfig, vcId = 0.U, nodeId.U, writeDestId, flitData)
        io.w.ready := io.routerLink.out.ready
        when(io.routerLink.out.ready) {
          writeState := 0.U
          io.b.valid := true.B
        }
      }
    }
  }

  // Read channel: send read request, receive read response
  val readState = RegInit(0.U(2.W))

  io.ar.ready := false.B
  io.r.valid := false.B
  io.r.bits := DontCare

  switch(readState) {
    is(0.U) {
      // Wait for read address
      when(io.ar.valid) {
        val readReq = Cat(io.ar.bits.addr, 0.U((config.dataWidth - io.ar.bits.addr.getWidth).W))
        io.routerLink.out.valid := true.B
        io.routerLink.out.bits := Flit.headTail(flitConfig, vcId = 0.U, nodeId.U, io.ar.bits.destId, readReq)
        io.ar.ready := io.routerLink.out.ready
        when(io.routerLink.out.ready) {
          readState := 1.U
        }
      }
    }
    is(1.U) {
      // Wait for read response
      when(io.routerLink.in.valid) {
        io.r.valid := true.B
        io.r.bits.data := io.routerLink.in.bits.data
        io.r.bits.last := true.B
        io.routerLink.in.ready := io.r.ready
        when(io.r.ready) {
          readState := 0.U
        }
      }
    }
  }
}
