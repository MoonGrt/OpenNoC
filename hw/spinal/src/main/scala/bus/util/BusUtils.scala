package opennoc.bus.util

import spinal.core._

class BusCounter(width: Int=32) extends Component {
  val enable=in Bool();val clear=in Bool();val value=out UInt(width bits)
  val count=Reg(UInt(width bits)) init(0)
  when(clear){count:=0}.elsewhen(enable){count:=count+1}
  value:=count
}

class BusPipeline(width:Int=32) extends Component {
  val sValid=in Bool();val sReady=out Bool();val sBits=in Bits(width bits)
  val mValid=out Bool();val mReady=in Bool();val mBits=out Bits(width bits)
  val full=RegInit(False);val data=Reg(Bits(width bits))
  sReady := !full || mReady;mValid:=full;mBits:=data
  when(sReady){full:=sValid;when(sValid){data:=sBits}}
}

class BusFifo(width:Int=32,depth:Int=4) extends Component {
  private val pw=log2Up(depth) max 1;private val cw=log2Up(depth+1)
  val sValid=in Bool();val sReady=out Bool();val sBits=in Bits(width bits)
  val mValid=out Bool();val mReady=in Bool();val mBits=out Bits(width bits)
  val occupancy=out UInt(cw bits)
  val mem=Vec(Reg(Bits(width bits)),depth)
  val rd=Reg(UInt(pw bits))init(0);val wr=Reg(UInt(pw bits))init(0)
  val count=Reg(UInt(cw bits))init(0)
  val push=sValid&&sReady;val pop=mValid&&mReady
  sReady:=count<depth;mValid:=count=/=0;mBits:=mem(rd);occupancy:=count
  when(push){mem(wr):=sBits;wr:=Mux(wr===depth-1,U(0),wr+1)}
  when(pop){rd:=Mux(rd===depth-1,U(0),rd+1)}
  switch(push##pop){is(B"10"){count:=count+1};is(B"01"){count:=count-1}}
}

class BusSkidBuffer(width:Int=32) extends Component {
  val sValid=in Bool();val sReady=out Bool();val sBits=in Bits(width bits)
  val mValid=out Bool();val mReady=in Bool();val mBits=out Bits(width bits)
  val held=RegInit(False);val data=Reg(Bits(width bits))
  sReady := !held;mValid:=held||sValid;mBits:=Mux(held,data,sBits)
  when(!held && sValid && !mReady){held:=True;data:=sBits}
  .elsewhen(held&&mReady){held:=False}
}
