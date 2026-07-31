package noc.switch

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import noc.config._

/**
 * CrossbarTest - Unit tests for Crossbar switch module
 */
class CrossbarTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Crossbar"

  val numInputs = 2
  val numOutputs = 2
  val config = NoCConfig(
    dataWidth    = 32,
    vcNum        = 1,  // Single virtual channel
    bufferDepth  = 4,  // Larger buffer for ring topology
    nodeIdWidth  = 2,  // Support up to 4 nodes
    routingType  = "Ring",
    topologyType = "Ring"
  )

  // Test 1: Single input → Single output (HeadTail flit)
  it should "route a flit correctly with packed Flit format" in {
    test(new Crossbar(config, 2, 2)) { dut =>

      dut.io.out.foreach(_.ready.poke(true.B))

      // input 0 -> output 1
      dut.io.select(0).poke(1.U)
      dut.io.select(1).poke(0.U)

      val flitBits = makeHeadTailFlit(
        config.flitConfig,
        flitType = FlitType.HeadTail.id,
        vcId     = 1,
        srcId    = 1,
        dstId    = 3,
        data     = 0x55
      )

      dut.io.in(0).valid.poke(true.B)
      dut.io.in(0).bits.flit.poke(flitBits.U)

      dut.io.in(1).valid.poke(false.B)

      dut.clock.step()

      dut.io.out(1).valid.expect(true.B)
      dut.io.out(1).bits.flit.expect(flitBits.U)
      dut.io.out(0).valid.expect(false.B)
    }
  }

  // Test 2: Multiple inputs → Different outputs (parallel)
  it should "route multiple packed flits in parallel" in {
    test(new Crossbar(config, 2, 2)) { dut =>

      dut.io.out.foreach(_.ready.poke(true.B))

      dut.io.select(0).poke(0.U)
      dut.io.select(1).poke(1.U)

      dut.io.in(0).valid.poke(true.B)
      dut.io.in(0).bits.flit.poke(0x111.U)

      dut.io.in(1).valid.poke(true.B)
      dut.io.in(1).bits.flit.poke(0x222.U)

      dut.clock.step()

      dut.io.out(0).bits.flit.expect(0x111.U)
      dut.io.out(1).bits.flit.expect(0x222.U)
    }
  }

  // Test 3: Competing for the same output (arbitration present)
  it should "arbitrate correctly when flits contend" in {
    test(new Crossbar(config, 2, 2)) { dut =>

      dut.io.out(0).ready.poke(true.B)

      dut.io.select(0).poke(0.U)
      dut.io.select(1).poke(0.U)

      dut.io.in(0).valid.poke(true.B)
      dut.io.in(0).bits.flit.poke(0xaaa.U)

      dut.io.in(1).valid.poke(true.B)
      dut.io.in(1).bits.flit.poke(0xbbb.U)

      dut.clock.step()

      dut.io.out(0).valid.expect(true.B)

      val out = dut.io.out(0).bits.flit.peek().litValue
      assert(out == 0xaaa || out == 0xbbb)
    }
  }

  // Helper function to create a head-tail flit with the given parameters
  def makeHeadTailFlit(
    config: FlitConfig,
    flitType: Int,
    vcId: Int,
    srcId: Int,
    dstId: Int,
    data: Int
  ): BigInt = {
    import config._

    var header = BigInt(0)

    val ft = config.bitsMap(HeaderType.FlitType)
    header |= BigInt(flitType) << ft.Offset

    val vc = config.bitsMap(HeaderType.VcId)
    header |= BigInt(vcId) << vc.Offset

    val src = config.bitsMap(HeaderType.SrcId)
    header |= BigInt(srcId) << src.Offset

    val dst = config.bitsMap(HeaderType.DstId)
    header |= BigInt(dstId) << dst.Offset

    (header << dataWidth) | BigInt(data)
  }
}
