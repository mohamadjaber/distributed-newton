package benchmarks
import java.io._
import distributed_newton_star.DistributedNewtonStarGraph
import distributed_newton_star.DistributedNewtonStarGraphLinear
import distributed_newton_star.DistributedNewtonStarGraphLogistic

/**
 * @author ${user.name}
 */
object AppNewtonTime {

  val pw = new PrintWriter(new File("bench/benchNewtonLinearTime"))

  def main(args: Array[String]) {
    val eta = 0.1
    val stepSize = 0.9
    val nbPartitions = 8
    val numberIterations = 100
    val innerIterations = 25
    val innerStepSize = 0.9
    pw.println("# nbFeatures - # partitions 8  (" + numberIterations + " iterations) - time in ms")
    for (i <- 0 until 10) {
      val nbFeatures = (i + 1) * 10
      pw.print(nbFeatures + "\t")
      benchNewtonLinear(nbFeatures, eta, stepSize, nbPartitions, numberIterations)
      // benchNewtonLogistic(nbFeatures, eta, stepSize, nbPartitions, numberIterations, innerIterations, innerStepSize)
    }
  }

  def reportBench(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int, gdn: DistributedNewtonStarGraph) {
    val t0 = System.currentTimeMillis()
    gdn.learning(numberIterations)
    val t1 = System.currentTimeMillis()
    pw.println((t1 - t0))
    pw.flush()
  }

  def benchNewtonLinear(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int) {
    val gdn = new DistributedNewtonStarGraphLinear(nbPartitions, eta, stepSize, "input/linear_features_" + nbFeatures)
    reportBench(nbFeatures, eta, stepSize, nbPartitions, numberIterations, gdn)
  }

  def benchNewtonLogistic(nbFeatures: Int, eta: Double, stepSize: Double, nbPartitions: Int, numberIterations: Int,
    innerIterations: Int, innerStepSize: Double) {
    val gdn = new DistributedNewtonStarGraphLogistic(nbPartitions, eta, stepSize, innerStepSize, innerIterations, "input/linear_features_" + nbFeatures)
    reportBench(nbFeatures, eta, stepSize, nbPartitions, numberIterations, gdn)
  }
  
}
