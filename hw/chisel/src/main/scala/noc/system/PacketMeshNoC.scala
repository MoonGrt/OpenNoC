package noc.system

import chisel3._
import chisel3.util._
import noc.config.{NoCConfig, Port}
import noc.ni.{PacketInputBeat, PacketOutputBeat, PacketStreamNI}
import noc.router.RouterBuilder
import noc.topology.MeshTopology

/** A real hop-by-hop 2-D mesh using the library XY routers. */
class PacketMeshNoC(config: NoCConfig, width: Int, height: Int) extends Module {
  require(width > 0 && height > 0)
  private val nodes = width * height
  val io = IO(new Bundle {
    val in = Flipped(Vec(nodes, Decoupled(new PacketInputBeat(config))))
    val out = Vec(nodes, Decoupled(new PacketOutputBeat(config)))
  })

  private val routers = Seq.fill(nodes)(RouterBuilder.buildXYRouter(config, width, height))
  private val interfaces =
    Seq.tabulate(nodes)(i => Module(new PacketStreamNI(config, i, nodes)))
  private val topology = new MeshTopology(config, width, height)

  for (i <- 0 until nodes) {
    routers(i).io.routerId := i.U
    routers(i).io.inPorts(Port.Local.id) <> interfaces(i).io.routerLink.out
    interfaces(i).io.routerLink.in <> routers(i).io.outPorts(Port.Local.id)
    io.in(i) <> interfaces(i).io.streamIn
    io.out(i) <> interfaces(i).io.streamOut
  }
  topology.connectRouters(routers.map(_.io))
}
