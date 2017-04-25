package distributed_newton_star

/**
 * @author ${user.name}
 */
object App {

  def main(args: Array[String]) {
    val dts = new DistributedNewtonStar(4, 0.001, "input/test-star-input")
    dts.learning(1000)
  }

}
