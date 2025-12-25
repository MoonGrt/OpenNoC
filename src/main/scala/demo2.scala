package demo

import spinal.core._

class demo2 extends Component {
  val io = new Bundle {
    val a = in UInt(4 bits)
    val b = in UInt(4 bits)
    val and = out UInt(4 bits)
  }

  io.and := io.a & io.b
}

object demo2 {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(new demo2())
  }
}
