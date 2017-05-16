package benchmarks

import breeze.linalg._
import breeze.numerics._

class GradientDescentLinear(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) extends GradientDescent(minNbPartitions, eta, stepSize, inputFilePath) {

  def computeGradient() = {
    val innerSum = rddData.map(item => (theta.t * item._2 - item._1) * item._2).reduce(_ + _)
    innerSum + eta * theta
  }

  def computeError() = {
    rddData.map(item => pow(item._1 - theta.t * item._2, 2)).reduce(_ + _)
  }

  def updateTheta() {
    val gradient = computeGradient()
    theta = theta - stepSize * gradient
    println(norm(gradient - eta * theta))
  }
}