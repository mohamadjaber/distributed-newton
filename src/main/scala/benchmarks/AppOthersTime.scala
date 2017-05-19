package benchmarks
import java.io._

/**
 * @author ${user.name}
 */

object AppOthersTime {
  // Make sure to use learning and not learningBench to compute accurate time

  // update file name
  val pw = new PrintWriter(new File("bench/benchGDLinearTime"))

  def main(args: Array[String]) {
    val eta = 0.1
    val stepSizes = Array[Double](0.0000005, 0.00000025, 0.00000025, 0.00000015, 0.0000001, 0.0000001, 0.0000001, 0.00000009, 0.00000008, 0.00000007)
    val numberIterations = 100
    pw.println("# nbFeatures - # partitions 8  (" + numberIterations + " iterations) - time in ms")
    for (i <- 9 until 10) {
      val nbFeatures = (i + 1) * 10
      pw.print(nbFeatures + "\t")
      val nbPartitions = 8 // or vary the number of partitions
      bench(nbFeatures, eta, stepSizes(i), nbPartitions, numberIterations)
      pw.println()
      pw.flush()
    }
    pw.close()
  }

  def bench(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int) {
    // update method accordingly
    val gdl = new GradientDescentLinearStochastic(nbPartitions, eta, stepSize, "input/linear_features_" + nbFeatures)

    val t0 = System.currentTimeMillis()
    gdl.learning(numberIterations)
    val t1 = System.currentTimeMillis()

    pw.print((t1 - t0) + " ")
  }
}
