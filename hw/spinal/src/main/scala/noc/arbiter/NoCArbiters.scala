package opennoc.noc.arbiter

import spinal.core._

class FixedPriorityArbiter(inputs: Int) extends Component {
  val request = in Bits(inputs bits)
  val grant = out Bits(inputs bits)
  val valid = out Bool()
  grant := 0; valid := False
  for (i <- (0 until inputs).reverse)
    when(request(i)) { grant := B(1 << i, inputs bits); valid := True }
}

class RoundRobinArbiter(inputs: Int) extends Component {
  private val indexWidth = log2Up(inputs) max 1
  val request = in Bits(inputs bits)
  val advance = in Bool()
  val grant = out Bits(inputs bits)
  val valid = out Bool()
  val grantIndex = out UInt(indexWidth bits)
  val next = Reg(UInt(indexWidth bits)) init(0)
  grant := 0; valid := False; grantIndex := next
  for (offset <- (0 until inputs).reverse) {
    val sum = next.resize(indexWidth + 1) + U(offset, indexWidth + 1 bits)
    val candidate = UInt(indexWidth bits)
    candidate := sum.resized
    when(sum >= inputs) { candidate := (sum - inputs).resized }
    when(request(candidate)) {
      grant := (B(1, inputs bits) |<< candidate).resize(inputs)
      grantIndex := candidate; valid := True
    }
  }
  when(advance && valid) {
    next := Mux(grantIndex === inputs - 1, U(0), grantIndex + 1)
  }
}
