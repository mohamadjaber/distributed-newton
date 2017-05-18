package distributed_newton_star.examples

import java.io._
import breeze.linalg._
import breeze.numerics._

object BuildTrainingSetLinearRegBench1 {
  def main(args: Array[String]) {
    val nbPoints = 1000000
    for(nbFeatures <- 10 to 100 by 10) {
      generate(nbPoints, nbFeatures, "linear_features_" + nbFeatures) 
    }
  }

  def generate(nbPoints: Int, nbFeatures: Int, file: String) {
    val tetaStar = DenseVector.rand[Double](nbFeatures)

    val pw = new PrintWriter(new File("input/" + file))
    val random = scala.util.Random
    var i = 0

    for (i <- 1 to nbPoints) {
      if (i % 100000 == 0) println(i)
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