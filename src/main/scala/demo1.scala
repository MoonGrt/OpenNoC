package demo

import spinal.core._

/* ---------------- demo1 ---------------- */
class demo1 extends Component {
  val io = new Bundle {
    val led = out Bool()
  }

  val CNT_MAX = 50000 - 1

  val cntReg = Reg(UInt(32 bits)) init(0)
  val blkReg = Reg(Bool()) init(False)

  cntReg := cntReg + 1
  when(cntReg === CNT_MAX) {
    cntReg := 0
    blkReg := ~blkReg
  }

  io.led := blkReg
}

object demo1 {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(new demo1())
  }
}
