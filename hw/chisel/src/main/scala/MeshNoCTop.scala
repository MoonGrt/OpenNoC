package noc.system

import chisel3._
import noc.config.NoCConfig

class MeshNoCTop(
    val MESH_X: Int = 2,
    val MESH_Y: Int = 2,
    val DATA_WIDTH: Int = 32,
    val NODE_ID_WIDTH: Int = 2,
    val VC_NUM: Int = 1,
    val BUFFER_DEPTH: Int = 2)
    extends Module {
  private val nodes = MESH_X * MESH_Y
  require(nodes > 0 && BigInt(nodes) <= (BigInt(1) << NODE_ID_WIDTH))
  private val config = NoCConfig(
    dataWidth = DATA_WIDTH,
    vcNum = VC_NUM,
    bufferDepth = BUFFER_DEPTH,
    nodeIdWidth = NODE_ID_WIDTH,
    routingType = "XY",
    topologyType = "Mesh")
  private val mesh = Module(new PacketMeshNoC(config, MESH_X, MESH_Y))

  val in_valid = IO(Input(UInt(nodes.W)))
  val in_ready = IO(Output(UInt(nodes.W)))
  val in_data = IO(Input(UInt((nodes * DATA_WIDTH).W)))
  val in_dest = IO(Input(UInt((nodes * NODE_ID_WIDTH).W)))
  val in_last = IO(Input(UInt(nodes.W)))
  val out_valid = IO(Output(UInt(nodes.W)))
  val out_ready = IO(Input(UInt(nodes.W)))
  val out_data = IO(Output(UInt((nodes * DATA_WIDTH).W)))
  val out_src = IO(Output(UInt((nodes * NODE_ID_WIDTH).W)))
  val out_dest = IO(Output(UInt((nodes * NODE_ID_WIDTH).W)))
  val out_last = IO(Output(UInt(nodes.W)))

  val readyVec = Wire(Vec(nodes, Bool()))
  val validVec = Wire(Vec(nodes, Bool()))
  val dataVec = Wire(Vec(nodes, UInt(DATA_WIDTH.W)))
  val srcVec = Wire(Vec(nodes, UInt(NODE_ID_WIDTH.W)))
  val destVec = Wire(Vec(nodes, UInt(NODE_ID_WIDTH.W)))
  val lastVec = Wire(Vec(nodes, Bool()))
  for (i <- 0 until nodes) {
    val requestedDest = in_dest((i + 1) * NODE_ID_WIDTH - 1, i * NODE_ID_WIDTH)
    mesh.io.in(i).valid := in_valid(i)
    mesh.io.in(i).bits.data := in_data((i + 1) * DATA_WIDTH - 1, i * DATA_WIDTH)
    mesh.io.in(i).bits.dest := requestedDest
    mesh.io.in(i).bits.last := in_last(i)
    readyVec(i) := mesh.io.in(i).ready
    validVec(i) := mesh.io.out(i).valid
    mesh.io.out(i).ready := out_ready(i)
    dataVec(i) := mesh.io.out(i).bits.data
    srcVec(i) := mesh.io.out(i).bits.src
    destVec(i) := mesh.io.out(i).bits.dest
    lastVec(i) := mesh.io.out(i).bits.last
  }
  in_ready := readyVec.asUInt
  out_valid := validVec.asUInt
  out_data := dataVec.asUInt
  out_src := srcVec.asUInt
  out_dest := destVec.asUInt
  out_last := lastVec.asUInt
}

object MeshNoCTop extends App {
  private def env(name: String, default: Int): Int =
    sys.env.get(name).map(_.toInt).getOrElse(default)
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new MeshNoCTop(
      env("MESH_X", 2), env("MESH_Y", 2), env("DATA_WIDTH", 32),
      env("NODE_ID_WIDTH", 2), env("VC_NUM", 1), env("BUFFER_DEPTH", 2)),
    Array("--target-dir", sys.env.getOrElse("RTL_TARGET_DIR", "out")),
    Array("--lowering-options=disallowLocalVariables,disallowPackedArrays"))
}
