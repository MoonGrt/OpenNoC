package opennoc.bus.demo

import spinal.core._

class BusSmokeTop extends Component {
  noIoPrefix()
  val req_valid=in Bool();val req_ready=out Bool();val req_write=in Bool()
  val req_addr=in UInt(8 bits);val req_wdata=in Bits(32 bits);val req_strb=in Bits(4 bits)
  val rsp_valid=out Bool();val rsp_ready=in Bool();val rsp_rdata=out Bits(32 bits)
  val rsp_error=out Bool()
  val tx_valid=out Bool();val tx_ready=in Bool();val tx_data=out Bits(8 bits)

  val ram=new BusRam(7,32);val uart=new BusUart(32)
  val active=RegInit(False);val uartSelected=RegInit(False)
  val requestUart=req_addr(7)

  req_ready := !active && Mux(requestUart,uart.reqReady,ram.reqReady)
  rsp_valid:=active&&Mux(uartSelected,uart.rspValid,ram.rspValid)
  rsp_rdata:=Mux(uartSelected,uart.rspRdata,ram.rspRdata)
  rsp_error:=Mux(uartSelected,False,ram.rspError)

  ram.reqValid:=req_valid && req_ready && !requestUart
  ram.reqWrite:=req_write;ram.reqAddr:=req_addr(6 downto 0)
  ram.reqWdata:=req_wdata;ram.reqStrb:=req_strb
  ram.rspReady:=active && !uartSelected && rsp_ready

  uart.reqValid:=req_valid&&req_ready&&requestUart
  uart.reqWrite:=req_write;uart.reqAddr:=req_addr(3 downto 2)
  uart.reqWdata:=req_wdata;uart.rspReady:=active&&uartSelected&&rsp_ready
  tx_valid:=uart.txValid;uart.txReady:=tx_ready;tx_data:=uart.txData

  when(req_valid&&req_ready){active:=True;uartSelected:=requestUart}
  when(rsp_valid&&rsp_ready){active:=False}
}

object BusSmokeTop extends App {
  SpinalConfig(
    targetDirectory=sys.env.getOrElse("RTL_TARGET_DIR","out"),
    defaultConfigForClockDomains=ClockDomainConfig(resetKind=SYNC)
  ).generateSystemVerilog(new BusSmokeTop)
}
