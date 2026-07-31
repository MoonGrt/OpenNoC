package opennoc.noc.channel

import spinal.core._

class PipelineChannel(width: Int) extends Component {
  val inValid = in Bool(); val inReady = out Bool(); val inBits = in Bits(width bits)
  val outValid = out Bool(); val outReady = in Bool(); val outBits = out Bits(width bits)
  val full = RegInit(False); val data = Reg(Bits(width bits))
  inReady := !full || outReady; outValid := full; outBits := data
  when(inReady) { full := inValid; when(inValid) { data := inBits } }
}

class BufferedChannel(width: Int, depth: Int) extends Component {
  private val ptrWidth = log2Up(depth) max 1
  private val cntWidth = log2Up(depth + 1)
  val inValid = in Bool(); val inReady = out Bool(); val inBits = in Bits(width bits)
  val outValid = out Bool(); val outReady = in Bool(); val outBits = out Bits(width bits)
  val mem = Vec(Reg(Bits(width bits)), depth)
  val rd = Reg(UInt(ptrWidth bits)) init(0); val wr = Reg(UInt(ptrWidth bits)) init(0)
  val count = Reg(UInt(cntWidth bits)) init(0)
  val push = inValid && inReady; val pop = outValid && outReady
  inReady := count < depth; outValid := count =/= 0; outBits := mem(rd)
  when(push) { mem(wr) := inBits; wr := Mux(wr === depth - 1, U(0), wr + 1) }
  when(pop) { rd := Mux(rd === depth - 1, U(0), rd + 1) }
  switch(push ## pop) {
    is(B"10") { count := count + 1 }
    is(B"01") { count := count - 1 }
  }
}
