package opennoc

import spinal.core._
import opennoc.bus.adapter._
import opennoc.bus.common._
import opennoc.bus.demo._
import opennoc.bus.fabric._
import opennoc.bus.util._
import opennoc.noc.arbiter._
import opennoc.noc.channel._
import opennoc.noc.routing._
import opennoc.noc.ni._
import opennoc.noc.pe._
import opennoc.noc.switch._
import opennoc.noc.router.{VirtualChannel, VCAllocator}

/** Elaborates every standalone native library component used by lint-all. */
object LibraryElaboration extends App {
  private val target = sys.env.getOrElse("RTL_TARGET_DIR", "out/library")
  private val generators: Seq[() => Component] = Seq(
    () => new FixedPriorityArbiter(5),
    () => new RoundRobinArbiter(5),
    () => new PipelineChannel(32),
    () => new BufferedChannel(32, 4),
    () => new XYRouting(2, 2, 0),
    () => new PacketIngress(),
    () => new PacketEgress(),
    () => new RandomPacketSource(),
    () => new PacketSink(),
    () => new PacketCrossbar(),
    () => new VirtualChannel(),
    () => new VCAllocator(2),
    () => new OpenNoCAxiStream(),
    () => new OpenNoCAxiLite(),
    () => new OpenNoCAxi4(),
    () => new OpenNoCApb4(),
    () => new OpenNoCAhbLite(),
    () => new OpenNoCTileLinkUL(),
    () => new OpenNoCWishbone(),
    () => new OpenNoCAvalonMM(),
    () => new OpenNoCSimpleBus(),
    () => new BusCounter(),
    () => new BusPipeline(),
    () => new BusFifo(),
    () => new BusSkidBuffer(),
    () => new AddressDecoder(32, Seq(
      AddressRegion(0x00000000L, 0xffff0000L),
      AddressRegion(0x10000000L, 0xffff0000L))),
    () => new FabricArbiter(4),
    () => new BusRam(),
    () => new BusRom(),
    () => new BusUart(),
    () => new BusWidthAdapter(),
    () => new SimpleBusToApb(),
    () => new BusHost())
  generators.foreach { generator =>
    SpinalConfig(targetDirectory = target).generateSystemVerilog(generator())
  }
}
