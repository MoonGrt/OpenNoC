package opennoc.noc.routing

import spinal.core._

class XYRouting(meshX: Int, nodeIdWidth: Int, routerId: Int) extends Component {
  val dest = in UInt(nodeIdWidth bits)
  val port = out UInt(3 bits)
  val dx = (dest % meshX).resize(nodeIdWidth)
  val dy = (dest / meshX).resize(nodeIdWidth)
  port := 0
  when(dest === routerId) { port := 0 }
    .elsewhen(dx > routerId % meshX) { port := 1 }
    .elsewhen(dx < routerId % meshX) { port := 2 }
    .elsewhen(dy < routerId / meshX) { port := 3 }
    .otherwise { port := 4 }
}
