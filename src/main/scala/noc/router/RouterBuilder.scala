package noc.router

import chisel3._
import noc.config.NoCConfig
import noc.routing.{RoutingPolicy, XYRouting, AdaptiveXYRouting}

/**
 * RouterBuilder - Router builder
 * Provides convenient methods to build different types of routers
 */
object RouterBuilder {
  /**
   * Build a router using XY routing
   */
  def buildXYRouter(config: NoCConfig, meshWidth: Int, meshHeight: Int): Router = {
    val routingPolicy: RoutingPolicy = new XYRouting(config, meshWidth, meshHeight)
    Module(new Router(config, routingPolicy))
  }

  /**
   * Build a router using adaptive XY routing
   */
  def buildAdaptiveXYRouter(config: NoCConfig, meshWidth: Int, meshHeight: Int): Router = {
    val routingPolicy: RoutingPolicy = new AdaptiveXYRouting(config, meshWidth, meshHeight)
    Module(new Router(config, routingPolicy))
  }

  /**
   * Build a router using custom routing policy
   */
  def buildRouter(config: NoCConfig, routingPolicy: RoutingPolicy): Router = {
    Module(new Router(config, routingPolicy))
  }
}
