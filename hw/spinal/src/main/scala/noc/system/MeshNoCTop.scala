package opennoc.noc.system

import spinal.core._
import opennoc.noc.config.NoCConfig
import opennoc.noc.router.MeshRouter

class MeshNoCTop(config: NoCConfig) extends Component {
  noIoPrefix()
  private val n = config.nodes
  private val ports = 5
  private val local = 0
  private val east = 1
  private val west = 2
  private val north = 3
  private val south = 4

  val in_valid = in Bits(n bits)
  val in_ready = out Bits(n bits)
  val in_data = in Bits(n * config.dataWidth bits)
  val in_dest = in Bits(n * config.nodeIdWidth bits)
  val in_last = in Bits(n bits)
  val out_valid = out Bits(n bits)
  val out_ready = in Bits(n bits)
  val out_data = out Bits(n * config.dataWidth bits)
  val out_src = out Bits(n * config.nodeIdWidth bits)
  val out_dest = out Bits(n * config.nodeIdWidth bits)
  val out_last = out Bits(n bits)

  val routers = Array.tabulate(n)(i => new MeshRouter(config, i))
  val inputActive = Vec(Reg(Bool()) init(False), n)
  val inputDest = Vec(Reg(UInt(config.nodeIdWidth bits)) init(0), n)

  for (i <- 0 until n) {
    val x = i % config.meshX
    val y = i / config.meshX
    val requested = in_dest(i * config.nodeIdWidth, config.nodeIdWidth bits).asUInt
    val effective = UInt(config.nodeIdWidth bits)
    effective := requested
    when(inputActive(i)) { effective := inputDest(i) }
    val legal = if (n == (1 << config.nodeIdWidth)) True
      else effective < U(n, config.nodeIdWidth bits)

    for (p <- 0 until ports) {
      if (p == local) {
        routers(i).in_valid(p) := in_valid(i) && legal
        routers(i).in_data(p * config.dataWidth, config.dataWidth bits) :=
          in_data(i * config.dataWidth, config.dataWidth bits)
        routers(i).in_src(p * config.nodeIdWidth, config.nodeIdWidth bits) :=
          U(i, config.nodeIdWidth bits).asBits
        routers(i).in_dest(p * config.nodeIdWidth, config.nodeIdWidth bits) := effective.asBits
        routers(i).in_last(p) := in_last(i)
        routers(i).out_ready(p) := out_ready(i)
      } else {
        val neighbor =
          if (p == east && x < config.meshX - 1) Some((i + 1, west))
          else if (p == west && x > 0) Some((i - 1, east))
          else if (p == north && y > 0) Some((i - config.meshX, south))
          else if (p == south && y < config.meshY - 1) Some((i + config.meshX, north))
          else None
        neighbor match {
          case Some((j, opposite)) =>
            routers(i).in_valid(p) := routers(j).out_valid(opposite)
            routers(i).in_data(p * config.dataWidth, config.dataWidth bits) :=
              routers(j).out_data(opposite * config.dataWidth, config.dataWidth bits)
            routers(i).in_src(p * config.nodeIdWidth, config.nodeIdWidth bits) :=
              routers(j).out_src(opposite * config.nodeIdWidth, config.nodeIdWidth bits)
            routers(i).in_dest(p * config.nodeIdWidth, config.nodeIdWidth bits) :=
              routers(j).out_dest(opposite * config.nodeIdWidth, config.nodeIdWidth bits)
            routers(i).in_last(p) := routers(j).out_last(opposite)
            routers(i).out_ready(p) := routers(j).in_ready(opposite)
          case None =>
            routers(i).in_valid(p) := False
            routers(i).in_data(p * config.dataWidth, config.dataWidth bits) := 0
            routers(i).in_src(p * config.nodeIdWidth, config.nodeIdWidth bits) := 0
            routers(i).in_dest(p * config.nodeIdWidth, config.nodeIdWidth bits) := 0
            routers(i).in_last(p) := False
            routers(i).out_ready(p) := False
        }
      }
    }

    in_ready(i) := routers(i).in_ready(local) && legal
    out_valid(i) := routers(i).out_valid(local)
    out_data(i * config.dataWidth, config.dataWidth bits) :=
      routers(i).out_data(local * config.dataWidth, config.dataWidth bits)
    out_src(i * config.nodeIdWidth, config.nodeIdWidth bits) :=
      routers(i).out_src(local * config.nodeIdWidth, config.nodeIdWidth bits)
    out_dest(i * config.nodeIdWidth, config.nodeIdWidth bits) :=
      routers(i).out_dest(local * config.nodeIdWidth, config.nodeIdWidth bits)
    out_last(i) := routers(i).out_last(local)

    when(in_valid(i) && in_ready(i)) {
      when(!inputActive(i) && !in_last(i)) {
        inputActive(i) := True
        inputDest(i) := requested
      }
      when(in_last(i)) { inputActive(i) := False }
    }
  }
}

object MeshNoCTop extends App {
  private def env(name: String, default: Int): Int =
    sys.env.get(name).map(_.toInt).getOrElse(default)
  val config = NoCConfig(
    env("MESH_X", 2), env("MESH_Y", 2), env("DATA_WIDTH", 32),
    env("NODE_ID_WIDTH", 2), env("VC_NUM", 1), env("BUFFER_DEPTH", 2))
  SpinalConfig(
    targetDirectory = sys.env.getOrElse("RTL_TARGET_DIR", "out"),
    defaultConfigForClockDomains = ClockDomainConfig(resetKind = SYNC)
  ).generateSystemVerilog(new MeshNoCTop(config))
}
