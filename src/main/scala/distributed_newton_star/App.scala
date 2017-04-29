package distributed_newton_star

/**
 * @author ${user.name}
 */
object App {

  def main(args: Array[String]) {
    // val dts = new DistributedNewtonStar(4, 0.01, "input/test-star-input")
    val dts = new DistributedNewtonStarLogistic(4, 0.01, "input/test-star-input")

    dts.learning(1000)
  }

}
