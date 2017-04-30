package distributed_newton_star.examples

import java.io._

object BuildTrainingSetLogisticRegTest1 {
  def main(args: Array[String]) {
    val pw = new PrintWriter(new File("input/logistic-test"))
    val random = scala.util.Random

    for (i <- 1 to 1000) {
      for (j <- 1 to 10) {
        pw.write("" + (random.nextDouble() / 10) + " ")
      }
      val label = random.nextInt(2)
      pw.write(label + "\n")
    }
    pw.close
  }
}