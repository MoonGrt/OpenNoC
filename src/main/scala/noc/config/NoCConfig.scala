package noc.config

/**
 * NoCConfig - Global NoC configuration
 * Contains all configuration parameters required for NoC design
 *
 * @param dataWidth Data bit width (flit bit width)
 * @param flitWidth Flit bit width (usually equals dataWidth)
 * @param vcNum Number of virtual channels
 * @param bufferDepth Buffer depth for each virtual channel
 * @param nodeIdWidth Bit width of node ID
 * @param numPorts Number of ports per router (excluding Local port)
 * @param routingType Routing algorithm type
 * @param topologyType Topology type
 */
case class NoCConfig(
  dataWidth: Int = 32,
  flitWidth: Int = 32,
  vcNum: Int = 1,
  bufferDepth: Int = 4,
  nodeIdWidth: Int = 8,
  numPorts: Int = 4,
  routingType: String = "XY",
  topologyType: String = "Mesh"
) {
  require(dataWidth > 0, "Data width must be positive")
  require(flitWidth > 0, "Flit width must be positive")
  require(vcNum > 0, "Virtual channel number must be positive")
  require(bufferDepth > 0, "Buffer depth must be positive")
  require(nodeIdWidth > 0, "NodeId width must be positive")
  require(numPorts > 0, "Number of ports must be positive")

  def maxNodes: Int = (1 << nodeIdWidth) - 1
  def totalPorts: Int = numPorts + 1  // Including Local port
  def portWidth: Int = chisel3.util.log2Ceil(totalPorts)
  def vcIdWidth: Int = chisel3.util.log2Ceil(vcNum)

  // Create sub-configuration objects
  def nodeIdConfig: NodeIdConfig = NodeIdConfig(nodeIdWidth)
  def portConfig: PortConfig = PortConfig(numPorts)
}
