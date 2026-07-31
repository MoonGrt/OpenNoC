package opennoc.bus.demo

import spinal.core._

class BusRam(addrWidth:Int=10,dataWidth:Int=32) extends Component {
  val reqValid=in Bool();val reqReady=out Bool();val reqWrite=in Bool()
  val reqAddr=in UInt(addrWidth bits);val reqWdata=in Bits(dataWidth bits)
  val reqStrb=in Bits(dataWidth/8 bits)
  val rspValid=out Bool();val rspReady=in Bool();val rspRdata=out Bits(dataWidth bits)
  val rspError=out Bool()
  val mem=Mem(Bits(dataWidth bits),1<<addrWidth);val pending=RegInit(False)
  val readQ=Reg(Bits(dataWidth bits))
  reqReady := !pending || rspReady;rspValid:=pending;rspRdata:=readQ;rspError:=False
  when(reqReady){pending:=reqValid;when(reqValid){
    readQ:=mem.readAsync(reqAddr)
    when(reqWrite){mem.write(address=reqAddr,data=reqWdata,mask=reqStrb)}
  }}
}

class BusRom(addrWidth:Int=10,dataWidth:Int=32) extends Component {
  val reqValid=in Bool();val reqReady=out Bool();val reqAddr=in UInt(addrWidth bits)
  val rspValid=out Bool();val rspReady=in Bool();val rspRdata=out Bits(dataWidth bits)
  val mem=Mem(Bits(dataWidth bits),1<<addrWidth);val pending=RegInit(False)
  val readQ=Reg(Bits(dataWidth bits))
  reqReady := !pending || rspReady;rspValid:=pending;rspRdata:=readQ
  when(reqReady){pending:=reqValid;when(reqValid){readQ:=mem.readAsync(reqAddr)}}
}

class BusUart(dataWidth:Int=32) extends Component {
  val reqValid=in Bool();val reqReady=out Bool();val reqWrite=in Bool()
  val reqAddr=in UInt(2 bits);val reqWdata=in Bits(dataWidth bits)
  val rspValid=out Bool();val rspReady=in Bool();val rspRdata=out Bits(dataWidth bits)
  val txValid=out Bool();val txReady=in Bool();val txData=out Bits(8 bits)
  val txFull=RegInit(False);val txQ=Reg(Bits(8 bits));val pending=RegInit(False)
  reqReady:=(!pending||rspReady)&&(!reqWrite||reqAddr=/=0 || !txFull)
  rspValid:=pending;rspRdata:=0;rspRdata(0) := !txFull
  txValid:=txFull;txData:=txQ
  when(txFull&&txReady){txFull:=False}
  when(pending&&rspReady){pending:=False}
  when(reqValid&&reqReady){pending:=True;when(reqWrite&&reqAddr===0){
    txFull:=True;txQ:=reqWdata(7 downto 0)
  }}
}
