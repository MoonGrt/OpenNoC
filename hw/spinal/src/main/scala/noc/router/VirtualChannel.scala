package opennoc.noc.router

import spinal.core._
import opennoc.noc.channel.BufferedChannel

class VirtualChannel(width:Int=32,depth:Int=2) extends BufferedChannel(width,depth)

class VCAllocator(vcNum:Int=1) extends Component {
  private val vw=log2Up(vcNum) max 1
  val available=in Bits(vcNum bits);val valid=out Bool();val vc=out UInt(vw bits)
  valid:=False;vc:=0
  for(i<-(0 until vcNum).reverse)when(available(i)){valid:=True;vc:=i}
}
