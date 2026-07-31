package opennoc.noc.router

import spinal.core._
import opennoc.noc.config.NoCConfig

class MeshRouter(config: NoCConfig, routerId: Int) extends Component {
  noIoPrefix()
  private val ports = 5
  private val ptrWidth = log2Up(config.bufferDepth) max 1
  private val cntWidth = log2Up(config.bufferDepth + 1)
  private val routerX = routerId % config.meshX
  private val routerY = routerId / config.meshX

  val in_valid = in Bits(ports bits)
  val in_ready = out Bits(ports bits)
  val in_data = in Bits(ports * config.dataWidth bits)
  val in_src = in Bits(ports * config.nodeIdWidth bits)
  val in_dest = in Bits(ports * config.nodeIdWidth bits)
  val in_last = in Bits(ports bits)
  val out_valid = out Bits(ports bits)
  val out_ready = in Bits(ports bits)
  val out_data = out Bits(ports * config.dataWidth bits)
  val out_src = out Bits(ports * config.nodeIdWidth bits)
  val out_dest = out Bits(ports * config.nodeIdWidth bits)
  val out_last = out Bits(ports bits)

  val dataMem = Vec(Vec(Reg(Bits(config.dataWidth bits)) init(0), config.bufferDepth), ports)
  val srcMem = Vec(Vec(Reg(UInt(config.nodeIdWidth bits)) init(0), config.bufferDepth), ports)
  val destMem = Vec(Vec(Reg(UInt(config.nodeIdWidth bits)) init(0), config.bufferDepth), ports)
  val lastMem = Vec(Vec(Reg(Bool()) init(False), config.bufferDepth), ports)
  val rdPtr = Vec(Reg(UInt(ptrWidth bits)) init(0), ports)
  val wrPtr = Vec(Reg(UInt(ptrWidth bits)) init(0), ports)
  val count = Vec(Reg(UInt(cntWidth bits)) init(0), ports)
  val locked = Vec(Reg(Bool()) init(False), ports)
  val owner = Vec(Reg(UInt(3 bits)) init(0), ports)
  val rr = Vec(Reg(UInt(3 bits)) init(0), ports)
  val selected = Vec(UInt(3 bits), ports)
  val selectedValid = Vec(Bool(), ports)
  val pop = Vec(Bool(), ports)

  out_valid := 0; out_data := 0; out_src := 0; out_dest := 0; out_last := 0
  pop.foreach(_ := False)
  for (p <- 0 until ports) in_ready(p) := count(p) < config.bufferDepth

  def routeFor(dest: UInt): UInt = {
    val result = UInt(3 bits)
    val dx = (dest % config.meshX).resize(config.nodeIdWidth)
    val dy = (dest / config.meshX).resize(config.nodeIdWidth)
    result := 0
    when(dest === routerId) { result := 0 }
      .elsewhen(dx > routerX) { result := 1 }
      .elsewhen(dx < routerX) { result := 2 }
      .elsewhen(dy < routerY) { result := 3 }
      .otherwise { result := 4 }
    result
  }

  for (o <- 0 until ports) {
    selected(o) := owner(o)
    selectedValid(o) := False
    when(locked(o)) {
      selectedValid(o) := count(owner(o)) =/= 0
    } otherwise {
      for (offset <- (0 until ports).reverse) {
        val sum = rr(o).resize(4) + U(offset, 4 bits)
        val candidate = UInt(3 bits)
        candidate := sum.resized
        when(sum >= ports) { candidate := (sum - ports).resized }
        when(count(candidate) =/= 0 &&
          routeFor(destMem(candidate)(rdPtr(candidate))) === o) {
          selected(o) := candidate
          selectedValid(o) := True
        }
      }
    }
    when(selectedValid(o)) {
      val s = selected(o)
      out_valid(o) := True
      out_data(o * config.dataWidth, config.dataWidth bits) := dataMem(s)(rdPtr(s))
      out_src(o * config.nodeIdWidth, config.nodeIdWidth bits) := srcMem(s)(rdPtr(s)).asBits
      out_dest(o * config.nodeIdWidth, config.nodeIdWidth bits) := destMem(s)(rdPtr(s)).asBits
      out_last(o) := lastMem(s)(rdPtr(s))
      pop(s) := out_ready(o)
    }
  }

  for (p <- 0 until ports) {
    val push = in_valid(p) && in_ready(p)
    when(push) {
      dataMem(p)(wrPtr(p)) := in_data(p * config.dataWidth, config.dataWidth bits)
      srcMem(p)(wrPtr(p)) := in_src(p * config.nodeIdWidth, config.nodeIdWidth bits).asUInt
      destMem(p)(wrPtr(p)) := in_dest(p * config.nodeIdWidth, config.nodeIdWidth bits).asUInt
      lastMem(p)(wrPtr(p)) := in_last(p)
      wrPtr(p) := Mux(wrPtr(p) === config.bufferDepth - 1, U(0), wrPtr(p) + 1)
    }
    when(pop(p)) {
      rdPtr(p) := Mux(rdPtr(p) === config.bufferDepth - 1, U(0), rdPtr(p) + 1)
    }
    switch(push ## pop(p)) {
      is(B"10") { count(p) := count(p) + 1 }
      is(B"01") { count(p) := count(p) - 1 }
    }
  }
  for (o <- 0 until ports) {
    when(!locked(o) && selectedValid(o)) {
      locked(o) := True
      owner(o) := selected(o)
    }
    when(selectedValid(o) && out_ready(o) &&
      lastMem(selected(o))(rdPtr(selected(o)))) {
      locked(o) := False
      rr(o) := Mux(selected(o) === ports - 1, U(0), selected(o) + 1)
    }
  }
}
