package benchmarks

import configuration._
import breeze.linalg._
import breeze.numerics._

abstract class GradientDescent(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) extends Serializable {

  val rddData = parseFile(inputFilePath, minNbPartitions).cache()
  val numberPoints = rddData.collect().size
  val numberPartitions = rddData.getNumPartitions
  val numberFeatures = rddData.first()._2.size
  var theta = DenseVector.rand[Double](numberFeatures) 

  def parseFile(filePath: String, minPartitions: Int) = {
    ClusterConfiguration.sc.textFile(filePath, minPartitions).map(v => {
      val split = v.split("\\s+").map(_.toDouble)
      val len = split.length
      (split(len - 1), DenseVector(split.slice(0, len - 1)))
    })
  }

  def updateTheta()
  def computeGradient(): DenseVector[Double]
  def computeError(): Double

  def learning(steps: Int) {
    // println("Initial parameters")
    // println(theta)
    // println("-------------------------------")
    for (iteration <- 0 until steps) {
      updateTheta()
      // println("iteration " + iteration)
      // println(theta)
    }
  }

}