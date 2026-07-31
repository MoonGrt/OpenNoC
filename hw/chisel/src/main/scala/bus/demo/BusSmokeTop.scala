package bus.demo

import chisel3._
import chisel3.util._

class BusSmokeTop extends Module {
  val req_valid=IO(Input(Bool()));val req_ready=IO(Output(Bool()))
  val req_write=IO(Input(Bool()));val req_addr=IO(Input(UInt(8.W)))
  val req_wdata=IO(Input(UInt(32.W)));val req_strb=IO(Input(UInt(4.W)))
  val rsp_valid=IO(Output(Bool()));val rsp_ready=IO(Input(Bool()))
  val rsp_rdata=IO(Output(UInt(32.W)));val rsp_error=IO(Output(Bool()))
  val tx_valid=IO(Output(Bool()));val tx_ready=IO(Input(Bool()))
  val tx_data=IO(Output(UInt(8.W)))

  val mem=Mem(128,Vec(4,UInt(8.W)))
  val active=RegInit(false.B);val uartSelected=RegInit(false.B)
  val response=Reg(UInt(32.W));val pending=RegInit(false.B)
  val txFull=RegInit(false.B);val txByte=Reg(UInt(8.W))
  val requestUart=req_addr(7)
  val uartCanAccept = !req_write || req_addr(3,2) =/= 0.U || !txFull

  req_ready := !active && Mux(requestUart,uartCanAccept,true.B)
  rsp_valid:=pending;rsp_rdata:=response;rsp_error:=false.B
  tx_valid:=txFull;tx_data:=txByte
  when(txFull&&tx_ready){txFull:=false.B}
  when(req_valid&&req_ready){
    active:=true.B;pending:=true.B;uartSelected:=requestUart
    when(requestUart){
      response:=Mux(req_addr(3,2)===1.U,Cat(0.U(31.W),!txFull),0.U)
      when(req_write&&req_addr(3,2)===0.U){txFull:=true.B;txByte:=req_wdata(7,0)}
    }.otherwise{
      val read=mem.read(req_addr(6,0))
      response:=Cat(read.reverse)
      when(req_write){
        val bytes=Wire(Vec(4,UInt(8.W)))
        for(i<-0 until 4){bytes(i):=req_wdata(8*i+7,8*i)}
        mem.write(req_addr(6,0),bytes,req_strb.asBools)
      }
    }
  }
  when(rsp_valid&&rsp_ready){active:=false.B;pending:=false.B}
}

object BusSmokeTop extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new BusSmokeTop,
    Array("--target-dir",sys.env.getOrElse("RTL_TARGET_DIR","out")))
}
