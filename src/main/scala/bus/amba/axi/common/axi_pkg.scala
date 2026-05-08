package bus.amba.axi.common

import chisel3._

/** Parameters for AXI4-Lite. */
case class AxiLiteParams(
    addrBits: Int = 32,
    dataBits: Int = 32,
) {
  require(addrBits > 0 && dataBits >= 8 && dataBits % 8 == 0)
  def strobeBits: Int = dataBits / 8
}

/** Parameters for full AXI4. */
case class AxiParams(
    addrBits: Int = 32,
    dataBits: Int = 32,
    idBits: Int = 4,
) {
  require(dataBits % 8 == 0)
  def strobeBits: Int = dataBits / 8
}
