package distributed_newton_star
import java.io._

/**
 * @author ${user.name}
 */
object App {
  def main(args: Array[String]) {
    val nbFeatures = 10
    val eta = 0.1
    val stepSize = 0.9
    val nbPartitions = 4
    val numberIterations = 10
    val innerIterations = 25
    val innerStepSize = 0.9
    benchNewtonLinear(nbFeatures, eta, stepSize, nbPartitions, numberIterations)
    // benchNewtonLogistic(nbFeatures, eta, stepSize, nbPartitions, numberIterations, innerIterations, innerStepSize)
  }

  def reportBench(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int, pw: PrintWriter, gdn: DistributedNewtonStarGraph) {
    pw.println("# nbFeatures = " + nbFeatures + " - eta = " + eta + " - step size = " + stepSize + " - number Partitions = " + nbPartitions)
    pw.println("# Iteration \t\t Gradient \t\t Error \t\t ConsensusError")

    val t0 = System.currentTimeMillis()
    val bench = gdn.learning(numberIterations)
    for (i <- 0 until numberIterations) {
      pw.println(i + "\t\t" + bench(i)(0) + "\t\t" + bench(i)(1) + "\t\t" + bench(i)(2))
    }
    val t1 = System.currentTimeMillis()

    pw.println("# Elapsed time: " + (t1 - t0) + "ms")
    pw.close()
  }

  def benchNewtonLinear(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int) {
    val pw = new PrintWriter(new File("bench/benchNewtonLinear_" + nbFeatures + "_" + nbPartitions))
    val gdn = new DistributedNewtonStarGraphLinear(nbPartitions, eta, stepSize, "input/linear_features_" + nbFeatures)
    reportBench(nbFeatures, eta, stepSize, nbPartitions, numberIterations, pw, gdn)
  }

  def benchNewtonLogistic(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int,
    innerIterations: Int, innerStepSize: Double) {
    val pw = new PrintWriter(new File("bench/benchNewtonLogistic_" + nbFeatures + "_" + nbPartitions))
    val gdn = new DistributedNewtonStarGraphLogistic(nbPartitions, eta, stepSize, innerStepSize, innerIterations, "input/linear_features_" + nbFeatures)
    reportBench(nbFeatures, eta, stepSize, nbPartitions, numberIterations, pw, gdn)
  }

  def tmpBench() {
    val eta = 0.1
    val globalStepSize = 0.9 // 0.9
    val localStepSizeLogistic = 0.9
    val innerIterationLogistic = 1
    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/linear_features_100")
    val dts = new DistributedNewtonStarGraphLogistic(16, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/linear_features_20")

    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-linear1")

    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-star-input")
    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-star-input")
    // val dts = new DistributedNewtonStarGraphLogistic(4, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/train-labels-formatted")
    // val dts = new DistributedNewtonStarGraphLogistic(4, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/logistic-test")
    dts.learning(100)
  }
}
