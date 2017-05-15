package benchmarks
import breeze.linalg._
import breeze.numerics._

class GradientDescentLogistic(minNbPartitions: Int,
  eta: Double,
  stepSize: Double,
  inputFilePath: String) extends GradientDescent(minNbPartitions, eta, stepSize, inputFilePath) {
  
  
  def computeGradient() = {
    val innerSum = rddData.map(item => (item._1 - sigmoid(item._2)) * item._2).reduce(_+_)
    innerSum + eta * theta
  }
  
  def sigmoid(input: DenseVector[Double]) = {
    val sigmoid = 1.0 / (1.0 + exp(-theta.t * input))
    sigmoid
  }
  
  def updateTheta() {
    val gradient = computeGradient()
    theta = theta + stepSize * gradient
    println(norm(gradient - eta * theta))
  }
}