package opennoc.noc.ni

import spinal.core._

class PacketIngress(dataWidth:Int=32,nodeIdWidth:Int=2,nodes:Int=4) extends Component {
  val sValid=in Bool();val sReady=out Bool();val sData=in Bits(dataWidth bits)
  val sDest=in UInt(nodeIdWidth bits);val sLast=in Bool()
  val mValid=out Bool();val mReady=in Bool();val mData=out Bits(dataWidth bits)
  val mDest=out UInt(nodeIdWidth bits);val mLast=out Bool()
  val active=RegInit(False);val destQ=Reg(UInt(nodeIdWidth bits))init(0)
  val effective=Mux(active,destQ,sDest)
  val legal=if(nodes==(1<<nodeIdWidth))True else effective<nodes
  mValid:=sValid&&legal;sReady:=mReady&&legal;mData:=sData;mDest:=effective;mLast:=sLast
  when(sValid&&sReady){
    when(!active && !sLast){active:=True;destQ:=sDest}
    when(sLast){active:=False}
  }
}

class PacketEgress(dataWidth:Int=32,nodeIdWidth:Int=2) extends Component {
  val sValid=in Bool();val sReady=out Bool();val sData=in Bits(dataWidth bits)
  val sSrc=in UInt(nodeIdWidth bits);val sDest=in UInt(nodeIdWidth bits);val sLast=in Bool()
  val mValid=out Bool();val mReady=in Bool();val mData=out Bits(dataWidth bits)
  val mSrc=out UInt(nodeIdWidth bits);val mDest=out UInt(nodeIdWidth bits);val mLast=out Bool()
  sReady:=mReady;mValid:=sValid;mData:=sData;mSrc:=sSrc;mDest:=sDest;mLast:=sLast
}
