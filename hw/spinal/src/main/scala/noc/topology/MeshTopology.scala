package opennoc.noc.topology

case class MeshTopology(width:Int,height:Int) {
  require(width>0&&height>0)
  val nodes:Int=width*height
  def nodeId(x:Int,y:Int):Int=y*width+x
  def x(id:Int):Int=id%width
  def y(id:Int):Int=id/width
  def east(id:Int):Option[Int]=if(x(id)<width-1)Some(id+1)else None
  def west(id:Int):Option[Int]=if(x(id)>0)Some(id-1)else None
  def north(id:Int):Option[Int]=if(y(id)>0)Some(id-width)else None
  def south(id:Int):Option[Int]=if(y(id)<height-1)Some(id+width)else None
}
