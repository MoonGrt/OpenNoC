package noc.config

/**
 * TopologyConfig - Router configuration parameters
 */
case class TopologyConfig(
  val topologyType: String = "Mesh",
  val bufferDepth: Int = 16,
  val channelType: String = "Buffer",
  val direction: String = "Bidirectional"
) {

}
