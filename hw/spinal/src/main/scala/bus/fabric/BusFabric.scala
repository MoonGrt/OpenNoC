package opennoc.bus.fabric

import spinal.core._

case class AddressRegion(base:BigInt,mask:BigInt)

class AddressDecoder(addrWidth:Int,regions:Seq[AddressRegion]) extends Component {
  private val iw=log2Up(regions.size) max 1
  val addr=in UInt(addrWidth bits);val select=out Bits(regions.size bits)
  val hit=out Bool();val index=out UInt(iw bits)
  select:=0;hit:=False;index:=0
  for((r,i)<-regions.zipWithIndex)
    when((addr & U(r.mask,addrWidth bits))===U(r.base&r.mask,addrWidth bits)){
      select(i):=True;hit:=True;index:=i
    }
}

class FabricArbiter(masters:Int) extends Component {
  private val iw=log2Up(masters) max 1
  val request=in Bits(masters bits);val complete=in Bool()
  val grant=out Bits(masters bits);val valid=out Bool();val index=out UInt(iw bits)
  val locked=RegInit(False);val owner=Reg(UInt(iw bits))init(0);val next=Reg(UInt(iw bits))init(0)
  grant:=0;valid:=False;index:=owner
  when(locked){grant(owner):=True;valid:=request(owner)}otherwise{
    for(offset<-(0 until masters).reverse){
      val sum=next.resize(iw+1)+U(offset,iw+1 bits);val candidate=UInt(iw bits)
      candidate:=sum.resized;when(sum>=masters){candidate:=(sum-masters).resized}
      when(request(candidate)){grant:=0;grant(candidate):=True;index:=candidate;valid:=True}
    }
  }
  when(!locked&&valid){locked:=True;owner:=index}
  when(locked&&complete){locked:=False;next:=Mux(owner===masters-1,U(0),owner+1)}
}
