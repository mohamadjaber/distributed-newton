package benchmarks
import breeze.linalg._
import breeze.numerics._

class GradientDescentLogistic(minNbPartitions: Int,
  eta: Double,
  stepSize: Double,
  inputFilePath: String) extends GradientDescent(minNbPartitions, eta, stepSize, inputFilePath) {
  
  def computeError() = {
    rddData.map(item => item._1 * log(sigmoid(item._2)) + (1 - item._1) * log(1 - sigmoid(item._2))).reduce(_+_)
  }
  
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
    theta = theta + stepSize * gradient / (1.0 * numberPoints)
    println("Error: " + computeError())
    println(norm(gradient - eta * theta))
  }
}