package demo

import chisel3._
import noc.config.NoCConfig
import noc.router.{Router, RouterBuilder}
import noc.routing.{XYRouting, AdaptiveXYRouting}

/**
 * CustomRouterExample - Example of creating custom routers
 *
 * This example demonstrates how to:
 * 1. Create routers with different routing policies
 * 2. Use RouterBuilder for convenience
 * 3. Create routers with custom routing policies
 */
class CustomRouterExample extends Module {
  val config = NoCConfig(
    dataWidth = 32,
    vcNum = 2,
    bufferDepth = 4,
    nodeIdWidth = 8,
    numPorts = 4
  )

  // Example 1: Create router using RouterBuilder with XY routing
  val xyRouter = RouterBuilder.buildXYRouter(config, meshWidth = 4, meshHeight = 4)

  // Example 2: Create router using RouterBuilder with adaptive XY routing
  val adaptiveRouter = RouterBuilder.buildAdaptiveXYRouter(config, meshWidth = 4, meshHeight = 4)

  // Example 3: Create router with custom routing policy
  val customRoutingPolicy = new XYRouting(config, meshWidth = 4, meshHeight = 4)
  val customRouter = RouterBuilder.buildRouter(config, customRoutingPolicy)

  // Example 4: Directly instantiate router with routing policy
  val directRouter = Module(new Router(config, customRoutingPolicy))

  // Set router ID
  xyRouter.io.routerId := 0.U
  adaptiveRouter.io.routerId := 1.U
  customRouter.io.routerId := 2.U
  directRouter.io.routerId := 3.U

  // Set congestion info (for adaptive routing)
  xyRouter.io.congestionInfo := VecInit(Seq.fill(config.totalPorts)(0.U(8.W)))
  adaptiveRouter.io.congestionInfo := VecInit(Seq.fill(config.totalPorts)(0.U(8.W)))
  customRouter.io.congestionInfo := VecInit(Seq.fill(config.totalPorts)(0.U(8.W)))
  directRouter.io.congestionInfo := VecInit(Seq.fill(config.totalPorts)(0.U(8.W)))
}

// object CustomRouterExample extends App {
//   (new chisel3.stage.ChiselStage).emitVerilog(new CustomRouterExample, Array("--target-dir", "rtl"))
// }



