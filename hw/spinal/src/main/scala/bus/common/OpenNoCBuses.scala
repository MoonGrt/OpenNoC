package opennoc.bus.common

import spinal.core._

case class StreamBeat(dataWidth: Int, destWidth: Int) extends Bundle {
  val data = Bits(dataWidth bits)
  val keep = Bits(dataWidth / 8 bits)
  val last = Bool()
  val dest = UInt(destWidth bits)
}

class OpenNoCAxiStream(dataWidth: Int = 32, destWidth: Int = 2) extends Component {
  val sValid = in Bool(); val sReady = out Bool()
  val sPayload = in(StreamBeat(dataWidth, destWidth))
  val mValid = out Bool(); val mReady = in Bool()
  val mPayload = out(StreamBeat(dataWidth, destWidth))
  val full = RegInit(False)
  val payload = Reg(StreamBeat(dataWidth, destWidth))
  sReady := !full || mReady
  when(sReady) {
    full := sValid
    when(sValid) { payload := sPayload }
  }
  mValid := full
  mPayload := payload
}

class OpenNoCAxiLite(addrWidth: Int = 32, dataWidth: Int = 32) extends Component {
  val awValid = in Bool(); val awReady = out Bool(); val awAddr = in UInt(addrWidth bits)
  val wValid = in Bool(); val wReady = out Bool(); val wData = in Bits(dataWidth bits)
  val wStrb = in Bits(dataWidth / 8 bits)
  val bValid = out Bool(); val bReady = in Bool(); val bResp = out Bits(2 bits)
  val arValid = in Bool(); val arReady = out Bool(); val arAddr = in UInt(addrWidth bits)
  val rValid = out Bool(); val rReady = in Bool(); val rData = out Bits(dataWidth bits)
  val rResp = out Bits(2 bits)
  awReady := !ClockDomain.current.isResetActive
  wReady := !ClockDomain.current.isResetActive
  arReady := !ClockDomain.current.isResetActive
  bValid := awValid && wValid; bResp := 0
  rValid := arValid; rData := 0; rResp := 0
}

class OpenNoCAxi4(addrWidth:Int=32,dataWidth:Int=32,idWidth:Int=1) extends Component {
  val awValid=in Bool();val awReady=out Bool();val awId=in UInt(idWidth bits)
  val awAddr=in UInt(addrWidth bits);val awLen=in UInt(8 bits);val awSize=in UInt(3 bits)
  val wValid=in Bool();val wReady=out Bool();val wData=in Bits(dataWidth bits)
  val wStrb=in Bits(dataWidth/8 bits);val wLast=in Bool()
  val bValid=out Bool();val bReady=in Bool();val bId=out UInt(idWidth bits);val bResp=out Bits(2 bits)
  val arValid=in Bool();val arReady=out Bool();val arId=in UInt(idWidth bits)
  val arAddr=in UInt(addrWidth bits);val arLen=in UInt(8 bits);val arSize=in UInt(3 bits)
  val rValid=out Bool();val rReady=in Bool();val rId=out UInt(idWidth bits)
  val rData=out Bits(dataWidth bits);val rResp=out Bits(2 bits);val rLast=out Bool()
  awReady := !ClockDomain.current.isResetActive&&wValid&&bReady
  wReady := !ClockDomain.current.isResetActive&&awValid&&bReady
  bValid := !ClockDomain.current.isResetActive&&awValid&&wValid;bId:=awId;bResp:=0
  arReady := !ClockDomain.current.isResetActive&&rReady
  rValid := !ClockDomain.current.isResetActive&&arValid;rId:=arId;rData:=0;rResp:=0;rLast:=True
}

class OpenNoCApb4(addrWidth: Int = 32, dataWidth: Int = 32) extends Component {
  val psel = in Bool(); val penable = in Bool(); val pwrite = in Bool()
  val paddr = in UInt(addrWidth bits); val pwdata = in Bits(dataWidth bits)
  val pstrb = in Bits(dataWidth / 8 bits)
  val pready = out Bool(); val prdata = out Bits(dataWidth bits); val pslverr = out Bool()
  pready := !ClockDomain.current.isResetActive
  prdata := 0; pslverr := False
}

class OpenNoCAhbLite(addrWidth:Int=32,dataWidth:Int=32) extends Component {
  val hsel=in Bool();val hwrite=in Bool();val hready=in Bool();val htrans=in Bits(2 bits)
  val hsize=in UInt(3 bits);val haddr=in UInt(addrWidth bits);val hwdata=in Bits(dataWidth bits)
  val hreadyout=out Bool();val hresp=out Bool();val hrdata=out Bits(dataWidth bits)
  hreadyout := !ClockDomain.current.isResetActive;hresp:=False;hrdata:=0
}

class OpenNoCTileLinkUL(addrWidth:Int=32,dataWidth:Int=32,sourceWidth:Int=4)
    extends Component {
  val aValid=in Bool();val aReady=out Bool();val aOpcode=in Bits(3 bits)
  val aParam=in Bits(3 bits);val aSize=in UInt(3 bits);val aSource=in UInt(sourceWidth bits)
  val aAddress=in UInt(addrWidth bits);val aMask=in Bits(dataWidth/8 bits);val aData=in Bits(dataWidth bits)
  val dValid=out Bool();val dReady=in Bool();val dOpcode=out Bits(3 bits)
  val dParam=out Bits(3 bits);val dSize=out UInt(3 bits);val dSource=out UInt(sourceWidth bits)
  val dData=out Bits(dataWidth bits);val dDenied=out Bool();val dCorrupt=out Bool()
  aReady := !ClockDomain.current.isResetActive&&dReady;dValid:=aValid
  dOpcode:=aOpcode;dParam:=0;dSize:=aSize;dSource:=aSource;dData:=0
  dDenied:=False;dCorrupt:=False
}

class OpenNoCWishbone(addrWidth: Int = 32, dataWidth: Int = 32) extends Component {
  val cyc = in Bool(); val stb = in Bool(); val we = in Bool()
  val addr = in UInt(addrWidth bits); val wdata = in Bits(dataWidth bits)
  val sel = in Bits(dataWidth / 8 bits)
  val ack = out Bool(); val err = out Bool(); val rdata = out Bits(dataWidth bits)
  ack := !ClockDomain.current.isResetActive && cyc && stb
  err := False; rdata := 0
}

class OpenNoCAvalonMM(addrWidth: Int = 32, dataWidth: Int = 32) extends Component {
  val read = in Bool(); val write = in Bool(); val address = in UInt(addrWidth bits)
  val writedata = in Bits(dataWidth bits); val byteenable = in Bits(dataWidth / 8 bits)
  val waitrequest = out Bool(); val readdatavalid = out Bool()
  val readdata = out Bits(dataWidth bits)
  waitrequest := ClockDomain.current.isResetActive
  readdatavalid := !ClockDomain.current.isResetActive && read
  readdata := 0
}

class OpenNoCSimpleBus(addrWidth: Int = 32, dataWidth: Int = 32) extends Component {
  val req = in Bool(); val write = in Bool(); val addr = in UInt(addrWidth bits)
  val wdata = in Bits(dataWidth bits)
  val ready = out Bool(); val error = out Bool(); val rdata = out Bits(dataWidth bits)
  ready := !ClockDomain.current.isResetActive && req
  error := False; rdata := 0
}
