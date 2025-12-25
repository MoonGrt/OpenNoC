package demo

import spinal.core._
import spinal.sim._
import scala.util.Random

object demo1Test {
  def main(args: Array[String]): Unit = {
    SimConfig.withWave.compile(new demo1).doSim { dut =>
      var ledStatus = -1
      println("Start the blinking LED")

      // Simulate for 100 cycles with each cycle having 10000 clock steps
      for (_ <- 0 until 100) {
        dut.clockDomain.forkStimulus(10) // Generate clock stimulus every 10ns
        dut.clockDomain.waitSampling(10000) // Wait for 10000 clock cycles

        // Get current LED value
        val ledNow = dut.io.led.toBoolean
        val s = if (ledNow) "*" else "o"

        // Print LED status when it changes
        if (ledStatus != ledNow) {
          println(s)
          ledStatus = ledNow
        }
      }
      println("\nEnd the blinking LED")
    }
  }
}
