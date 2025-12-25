import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import demo._

class demo1Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "demo1"
  it should "pass" in {
    test(new demo1) { c =>
      c.clock.setTimeout(0)
      var ledStatus = BigInt(-1)
      println("Start the blinking LED")
      for (_ <- 0 until 100) {
        c.clock.step(10000)
        val ledNow = c.io.led.peek().litValue
        val s = if (ledNow == 0) "o" else "*"
        if (ledStatus != ledNow) {
          System.out.println(s)
          ledStatus = ledNow
        }
      }
      println("\nEnd the blinking LED")
    }
  }
}
