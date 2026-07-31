package opennoc.noc.config

case class NoCConfig(
    meshX: Int = 2,
    meshY: Int = 2,
    dataWidth: Int = 32,
    nodeIdWidth: Int = 2,
    vcNum: Int = 1,
    bufferDepth: Int = 2) {
  require(meshX > 0 && meshY > 0 && dataWidth > 0)
  require(nodeIdWidth > 0 && vcNum > 0 && bufferDepth > 0)
  require(meshX * meshY <= (BigInt(1) << nodeIdWidth))
  val nodes: Int = meshX * meshY
}
