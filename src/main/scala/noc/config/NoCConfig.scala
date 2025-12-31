package noc.config

/**
 * NoCConfig - Global NoC configuration
 * Contains all configuration parameters required for NoC design
 *
 * @param dataWidth Data bit width (flit bit width)
 * @param vcNum Number of virtual channels
 * @param bufferDepth Buffer depth for each virtual channel
 * @param nodeIdWidth Bit width of node ID
 * @param routingType Routing algorithm type
 * @param topologyType Topology type
 */
case class NoCConfig(
  dataWidth: Int = 32,
  vcNum: Int = 1,
  bufferDepth: Int = 4,
  nodeIdWidth: Int = 8,
  routingType: String = "XY",
  topologyType: String = "Mesh"
) {
  require(dataWidth > 0, "Data width must be positive")
  require(vcNum > 0, "Virtual channel number must be positive")
  require(bufferDepth > 0, "Buffer depth must be positive")
  require(nodeIdWidth > 0, "NodeId width must be positive")

  def maxNodes: Int = (1 << nodeIdWidth) - 1
  def portNum: Int = routerConfig.portConfig.portNum
  def portWidth: Int = chisel3.util.log2Ceil(portNum)
  def vcIdWidth: Int = chisel3.util.log2Ceil(vcNum)

  // Select the input port based on the topologyType.
  val selectedPortConfig: Seq[Port.port] = topologyType match {
    case "Ring"  => Port.dirEW
    case "Mesh" => Port.dirEWNS
    case "Torus" => Port.dirEWNS
    case "Cube" => Port.dirEWNSUD
    case _ => throw new IllegalArgumentException(s"Unknown topology type: $topologyType")
  }

  // Create sub-configuration objects
  val flitConfig:
    FlitConfig = FlitConfig(
      headerFields = Seq(
        HeaderField(HeaderType.FlitType, HeaderType.width),
        HeaderField(HeaderType.VcId,  vcIdWidth),
        HeaderField(HeaderType.DstId, nodeIdWidth)
      ),
      dataWidth = 32
    )
  val routerConfig:
    RouterConfig = RouterConfig(
      ports = selectedPortConfig,
      routingPolicy = routingType,
      bufferDepth = bufferDepth,
      vcNum = vcNum,
      vcArbiterType = "RoundRobin",
      switchArbiterType = "RoundRobin"
    )
  val topologyConfig:
    TopologyConfig = TopologyConfig(
      topologyType = topologyType,
      bufferDepth = bufferDepth
    )
}
