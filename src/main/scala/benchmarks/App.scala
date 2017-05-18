package benchmarks
import java.io._

/**
 * @author ${user.name}
 */

object App {

  def main(args: Array[String]) {
    val eta = 0.1
    val stepSizes = Array[Double](0.0000005, 0.00000025, 0.00000025, 0.00000015, 0.0000001, 0.0000001, 0.0000001, 0.00000009, 0.00000008, 0.00000007)
    val nbPartitions = 8
    for (i <- 0 until 10) { 
      val nbFeatures = (i + 1) * 10
      val numberIterations = (i + 1) * 1000
      bench(nbFeatures, eta, stepSizes(i), nbPartitions, numberIterations)
    }
  }

  def bench(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int) {
    val pw = new PrintWriter(new File("bench/benchGDLinear_" + nbFeatures + "_" + nbPartitions))
    val gdl = new GradientDescentLinear(nbPartitions, eta, stepSize, "input/linear_features_" + nbFeatures)
    pw.println("# nbFeatures = " + nbFeatures + " - eta = " + eta + " - step size = " + stepSize + " - number Partitions = " + nbPartitions)
    pw.println("# Iteration \t\t Gradient \t\t Error")

    val t0 = System.currentTimeMillis()
    val bench = gdl.learning(numberIterations)
    val t1 = System.currentTimeMillis()

    for (i <- 0 until numberIterations) {
      pw.println(i + "\t\t" + bench(i)(0) + "\t\t" + bench(i)(1))
    }

    pw.println("# Elapsed time: " + (t1 - t0) + "ms")
    pw.close()
  }

  def tmpBench() {
    val eta = 0.1
    val stepSize = 0.0000001

    // val gdl = new GradientDescentLinear(4, eta, stepSize, "input/test-star-input", pw)
    // val gdl = new GradientDescentLogistic(4, eta, stepSize, "input/train-labels-formatted", pw)
    // val gdl = new GradientDescentLogistic(4, eta, stepSize, "input/logistic-test", pw)
    // val gdl = new GradientDescentLinear(4, eta, stepSize, "input/test-linear1", pw)

    val gdl = new GradientDescentLinearStochastic(4, eta, stepSize, "input/linear_features_100")

    gdl.learning(100000)
  }
}
