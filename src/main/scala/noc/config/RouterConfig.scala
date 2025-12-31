package noc.config

/**
 * VCConfig - Virtual Channel configuration parameters
 */
case class VCConfig(
  val bufferDepth: Int = 16,
  val vcNum: Int = 4,
  val arbiterType: String = "RoundRobin",
  val vcAllocPolicy: String = "static"
) {
  require(bufferDepth > 0, "Buffer depth must be greater than 0")
}

/**
 * RouterConfig - Router configuration parameters
 */
case class RouterConfig(
  val ports: Seq[Port.port] = Port.dirEWNS,
  val routingPolicy: String = "XY",
  val creditWidth: Int = 0,
  val bufferDepth: Int = 16,
  val vcNum: Int = 4,
  val vcArbiterType: String = "RoundRobin",
  val switchArbiterType: String = "RoundRobin",
) {
  val portConfig: PortConfig = PortConfig(ports)
  val vcConfig: VCConfig = VCConfig(bufferDepth, vcNum, vcArbiterType, vcAllocPolicy = "static")
}
