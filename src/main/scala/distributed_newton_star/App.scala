package distributed_newton_star

/**
 * @author ${user.name}
 */
object App {
  def main(args: Array[String]) {
    val eta = 0.1
    val globalStepSize = 0.1 // 0.9
    val localStepSizeLogistic = 0.9
    val innerIterationLogistic = 30
    val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-linear1")

    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-star-input")
    // val dts = new DistributedNewtonStarGraphLinear(4, eta, globalStepSize, "input/test-star-input")
    // val dts = new DistributedNewtonStarGraphLogistic(4, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/train-labels-formatted")
    //  val dts = new DistributedNewtonStarGraphLogistic(4, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/logistic-test")
    dts.learning(100)
  }
}
