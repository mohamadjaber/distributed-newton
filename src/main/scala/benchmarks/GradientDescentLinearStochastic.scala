package benchmarks

import breeze.linalg._
import breeze.numerics._
import java.io._

class GradientDescentLinearStochastic(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) extends GradientDescent(minNbPartitions, eta, stepSize, inputFilePath) {

  def computeGradient() = {
    val innerSum = rddData.map(item => (theta.t * item._2 - item._1) * item._2).reduce(_ + _)
    innerSum + eta * theta
  }

  def computeGradientPartitions() = {
    val innerSum = rddData.mapPartitionsWithIndex((indexPartition, iterator) => {
      iterator.map(item => (indexPartition, (theta.t * item._2 - item._1) * item._2))
    }, false)
    innerSum.reduceByKey(_ + _).map(item => (item._1, item._2 + eta * theta)).collect()
    //inner
  }

  def computeError() = {
    rddData.map(item => pow(item._1 - theta.t * item._2, 2)).reduce(_ + _)
  }

  def updateTheta(bench: Array[Array[Double]], step: Int) {
    val localGradients = computeGradientPartitions()
    for (i <- 0 until localGradients.size) {
      theta = theta - stepSize * localGradients(i)._2
    }
    bench(step)(0) = norm(computeGradient() - eta * theta) 
    bench(step)(1) = computeError()
  }
}