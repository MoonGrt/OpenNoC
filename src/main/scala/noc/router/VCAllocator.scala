package noc.router

import chisel3._
import noc.config.NoCConfig

/**
 * VCAllocator - Static VC Allocator
 * This is a static vc allocator, where vcIdIn = vcIdOut. Currently VCA is NOT in charge of credit flow control any more!
 *
 * @param config NoC configuration
 * @param numInputs Number of inputs
 */
class VCAllocator(val config: NoCConfig) extends Module {
  val io = new Bundle {
    // val credit  = Input(Vec(config.portNum, UInt(config.vcIdWidth.W)))
    val vcIdIn  = Input(Vec(config.portNum, UInt(config.vcIdWidth.W)))
    val vcIdOut = Output(Vec(config.portNum, UInt(config.vcIdWidth.W)))
  }

  // val creditRegs = Vec.fill(NUM_OF_VC) {Module(new CreditReg()).io}

  // for (i <- 0 until NUM_OF_VC) {
  //     creditRegs(i).inc := io.credit(i)
  //     creditRegs(i).dec := (io.vcIdIn === UInt(i)) && io.vcIdOutValid
  // }
  // io.vcIdOutValid := (creditRegs(io.vcIdOut).creditOut != UInt(0) || io.credit(io.vcIdOut))

  for (i <- 0 until config.portNum) {
    io.vcIdOut(i) := io.vcIdIn(i)
  }
}
