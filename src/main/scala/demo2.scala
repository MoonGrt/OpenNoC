package demo

import chisel3._
import chisel3.stage._

class demo2 extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))    
    val b = Input(UInt(4.W))    
    val and = Output(UInt(4.W)) 
  })

  io.and := io.a & io.b
}

object demo2 extends App {
  (new ChiselStage).emitVerilog(new demo2, Array("--target-dir", "rtl"))
}
