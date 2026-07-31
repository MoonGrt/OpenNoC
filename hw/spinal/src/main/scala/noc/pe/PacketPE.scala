package opennoc.noc.pe

import spinal.core._

class RandomPacketSource(dataWidth:Int=32,nodeIdWidth:Int=2,nodes:Int=4) extends Component {
  require(dataWidth>=32)
  val enable=in Bool();val valid=out Bool();val ready=in Bool()
  val data=out Bits(dataWidth bits);val dest=out UInt(nodeIdWidth bits);val last=out Bool()
  val lfsr=Reg(Bits(32 bits))init(1)
  valid:=enable;data:=lfsr.resize(dataWidth);dest:=(lfsr.asUInt%nodes).resized;last:=lfsr(4 downto 3)===0
  when(valid&&ready){lfsr:=lfsr(30 downto 0)##(lfsr(31)^lfsr(21)^lfsr(1)^lfsr(0))}
}

class PacketSink(dataWidth:Int=32,nodeIdWidth:Int=2) extends Component {
  val valid=in Bool();val ready=out Bool();val data=in Bits(dataWidth bits)
  val src=in UInt(nodeIdWidth bits);val dest=in UInt(nodeIdWidth bits);val last=in Bool()
  val stall=in Bool();val flitCount=out UInt(32 bits);val packetCount=out UInt(32 bits)
  val flits=Reg(UInt(32 bits))init(0);val packets=Reg(UInt(32 bits))init(0)
  ready := !stall;when(valid&&ready){flits:=flits+1;when(last){packets:=packets+1}}
  flitCount:=flits;packetCount:=packets
}
