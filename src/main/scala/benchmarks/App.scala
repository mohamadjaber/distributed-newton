package benchmarks

/**
 * @author ${user.name}
 */
object App {
  def main(args: Array[String]) {
    val eta = 0.1
    val stepSize = 0.00005

    //  val gdl = new GradientDescentLinear(4, eta, stepSize, "input/test-star-input")
      val gdl = new GradientDescentLogistic(4, eta, stepSize, "input/train-labels-formatted")
    //  val gdl = new GradientDescentLogistic(4, eta, stepSize, "input/logistic-test")
    //val gdl = new GradientDescentLinear(4, eta, stepSize, "input/test-linear1")

    gdl.learning(100000)
  }
}
