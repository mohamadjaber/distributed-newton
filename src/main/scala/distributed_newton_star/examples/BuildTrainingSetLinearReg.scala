package distributed_newton_star.examples

import java.io._

object BuildInput {
  def main(args: Array[String]) {
    val pw = new PrintWriter(new File("input/test-star-input"))
    val random = scala.util.Random
    var i = 0
    
    for(i <- 1 to 10000) {
      val x1 = random.nextDouble()
      val x2 = random.nextDouble()
      val x3 = random.nextDouble()
      val y = x1 + (2 * x2) + (3 * x3) + random.nextDouble() / 10
      pw.write(x1 + " " + x2 + " "  + x3 + " " + y + "\n")
    }
    pw.close
  }
}