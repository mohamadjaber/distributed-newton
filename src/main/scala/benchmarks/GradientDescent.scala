package benchmarks

import configuration._
import breeze.linalg._
import breeze.numerics._
import java.io._

abstract class GradientDescent(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) extends Serializable {

  val rddData = parseFile(inputFilePath).repartition(minNbPartitions).cache()
  val numberPoints = rddData.collect().size
  val numberPartitions = rddData.getNumPartitions
  val numberFeatures = rddData.first()._2.size
  var theta = DenseVector.rand[Double](numberFeatures) 

  def parseFile(filePath: String) = {
    ClusterConfiguration.sc.textFile(filePath).map(v => {
      val split = v.split("\\s+").map(_.toDouble)
      val len = split.length
      (split(len - 1), DenseVector(split.slice(0, len - 1)))
    })
  }

  def updateTheta(bench: Array[Array[Double]], steps: Int)
  def computeGradient(): DenseVector[Double]
  def computeError(): Double

  def learning(steps: Int) = {
    val bench = Array.ofDim[Double](steps, 2) // this array holds gradient and error
    for (iteration <- 0 until steps) {
      if((iteration != 0) && (iteration % 100 == 0)) println("done " + iteration + " iterations")
      updateTheta(bench, iteration)
    }
    bench
  }

}