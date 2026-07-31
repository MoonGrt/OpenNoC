package opennoc.bus.adapter

import spinal.core._

class BusWidthAdapter(inWidth:Int=32,outWidth:Int=64) extends Component {
  require(outWidth>=inWidth&&outWidth%inWidth==0)
  private val ratio=outWidth/inWidth;private val cw=log2Up(ratio) max 1
  val sValid=in Bool();val sReady=out Bool();val sData=in Bits(inWidth bits)
  val mValid=out Bool();val mReady=in Bool();val mData=out Bits(outWidth bits)
  val buffer=Reg(Bits(outWidth bits));val count=Reg(UInt(cw bits))init(0)
  val full=RegInit(False)
  sReady := !full;mValid:=full;mData:=buffer
  when(sValid&&sReady){
    buffer((count*inWidth).resize(log2Up(outWidth)),inWidth bits):=sData
    when(count===ratio-1){count:=0;full:=True}otherwise{count:=count+1}
  }
  when(full&&mReady){full:=False}
}

class SimpleBusToApb(addrWidth:Int=32,dataWidth:Int=32) extends Component {
  val reqValid=in Bool();val reqReady=out Bool();val reqWrite=in Bool()
  val reqAddr=in UInt(addrWidth bits);val reqWdata=in Bits(dataWidth bits)
  val rspValid=out Bool();val rspReady=in Bool();val rspRdata=out Bits(dataWidth bits)
  val psel=out Bool();val penable=out Bool();val pwrite=out Bool()
  val paddr=out UInt(addrWidth bits);val pwdata=out Bits(dataWidth bits)
  val pready=in Bool();val prdata=in Bits(dataWidth bits)
  val state=Reg(UInt(2 bits))init(0);val writeQ=Reg(Bool());val addrQ=Reg(UInt(addrWidth bits))
  val wdataQ=Reg(Bits(dataWidth bits));val rdataQ=Reg(Bits(dataWidth bits))
  reqReady:=state===0;rspValid:=state===3;rspRdata:=rdataQ
  psel:=state===1||state===2;penable:=state===2;pwrite:=writeQ;paddr:=addrQ;pwdata:=wdataQ
  switch(state){
    is(0){when(reqValid){writeQ:=reqWrite;addrQ:=reqAddr;wdataQ:=reqWdata;state:=1}}
    is(1){state:=2}
    is(2){when(pready){rdataQ:=prdata;state:=3}}
    is(3){when(rspReady){state:=0}}
  }
}

class BusHost(addrWidth:Int=32,dataWidth:Int=32) extends Component {
  val start=in Bool();val write=in Bool();val address=in UInt(addrWidth bits)
  val writeData=in Bits(dataWidth bits);val busy=out Bool();val done=out Bool()
  val reqValid=out Bool();val reqReady=in Bool();val reqWrite=out Bool()
  val reqAddr=out UInt(addrWidth bits);val reqWdata=out Bits(dataWidth bits)
  val rspValid=in Bool();val rspReady=out Bool();val rspRdata=in Bits(dataWidth bits)
  val readData=out Bits(dataWidth bits)
  val active=RegInit(False);val sent=RegInit(False);val readQ=Reg(Bits(dataWidth bits))init(0)
  reqValid:=active && !sent;reqWrite:=write;reqAddr:=address;reqWdata:=writeData
  rspReady:=active&&sent;done:=rspValid&&rspReady;busy:=active;readData:=readQ
  when(start && !active){active:=True;sent:=False}
  when(reqValid&&reqReady){sent:=True}
  when(done){active:=False;sent:=False;readQ:=rspRdata}
}
