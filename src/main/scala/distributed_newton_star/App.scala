package distributed_newton_star

/**
 * @author ${user.name}
 */
object App {
  def main(args: Array[String]) {
    val eta = 0.1
    val globalStepSize = 0.005
    val localStepSizeLogistic = 0.5
    val innerIterationLogistic = 100

    // val dts = new DistributedNewtonStarGraphLinear(4, eta, learningRate, "input/test-star-input")
    val dts = new DistributedNewtonStarGraphLogistic(4, eta, globalStepSize, localStepSizeLogistic, innerIterationLogistic, "input/train-labels-formatted")
    // val dts = new DistributedNewtonStarGraphLogistic(4, eta, learningRate, alpha, innerIterationLogistic, "input/logistic-test")
    dts.learning(100)
  }
}
