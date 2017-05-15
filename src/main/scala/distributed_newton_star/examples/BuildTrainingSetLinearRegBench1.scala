package distributed_newton_star.examples

import java.io._
import breeze.linalg._
import breeze.numerics._

object BuildTrainingSetLinearRegBench1 {
  def main(args: Array[String]) {
    val nbPoints = 100000
    val nbFeatures = 50
    val tetaStar = DenseVector.rand[Double](nbFeatures)

    val pw = new PrintWriter(new File("input/test-linear1"))
    val random = scala.util.Random
    var i = 0

    for (i <- 1 to nbPoints) {
      if(i % 10000 == 0) println(i)
      val x = DenseVector.rand[Double](nbFeatures)
      val y = tetaStar.t * x + random.nextDouble() / 10
      for (j <- 0 until nbFeatures) {
        pw.write(x(j) + " ")
      }
      pw.write(y + "\n")
    }
    pw.close
  }
}