package opennoc.noc.switch

import spinal.core._

class PacketCrossbar(ports:Int=5,dataWidth:Int=32) extends Component {
  private val iw=log2Up(ports) max 1
  val inValid=in Bits(ports bits);val inReady=out Bits(ports bits)
  val inData=in Bits(ports*dataWidth bits);val inRoute=in Bits(ports*iw bits)
  val inLast=in Bits(ports bits)
  val outValid=out Bits(ports bits);val outReady=in Bits(ports bits)
  val outData=out Bits(ports*dataWidth bits);val outLast=out Bits(ports bits)
  val locked=Vec(Reg(Bool())init(False),ports);val owner=Vec(Reg(UInt(iw bits))init(0),ports)
  val selected=Vec(UInt(iw bits),ports);val selectedValid=Vec(Bool(),ports)
  inReady:=0;outValid:=0;outData:=0;outLast:=0
  for(o<-0 until ports){
    selected(o):=owner(o);selectedValid(o):=False
    when(locked(o)){selectedValid(o):=inValid(owner(o))}otherwise{
      for(i<-(0 until ports).reverse)when(inValid(i)&&
        inRoute(i*iw,iw bits).asUInt===o){selected(o):=i;selectedValid(o):=True}
    }
    when(selectedValid(o)){
      outValid(o):=True
      outData(o*dataWidth,dataWidth bits):=inData(selected(o)*dataWidth,dataWidth bits)
      outLast(o):=inLast(selected(o));inReady(selected(o)):=outReady(o)
    }
    when(!locked(o)&&selectedValid(o)){locked(o):=True;owner(o):=selected(o)}
    when(selectedValid(o)&&outReady(o)&&inLast(selected(o))){locked(o):=False}
  }
}
