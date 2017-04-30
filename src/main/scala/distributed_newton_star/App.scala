package distributed_newton_star

/**
 * @author ${user.name}
 */
object App {

  def main(args: Array[String]) {
    // val dts = new DistributedNewtonStarGraphLinear(4, 0.01, 0.005, "input/test-star-input")
    // val dts = new DistributedNewtonStarGraphLogistic(4, 0.01, 0.01, 0.01, 100, "input/train-labels-formatted")
    val dts = new DistributedNewtonStarGraphLogistic(4, 0.3, 0.005, 0.01, 300, "input/logistic-test")
    dts.learning(1000)
  }

}
