package distributed_newton_star

import breeze.linalg._
import breeze.numerics._

class DistributedNewtonStarGraphLinear(minNbPartitions: Int,
  eta: Double,
  stepSize: Double,
  inputFilePath: String)
    extends DistributedNewtonStarGraph(minNbPartitions, eta, stepSize, inputFilePath) {

  val rDDPPrimalDual = computeRDDPPrimalDual().cache()
  val localPPrimalDualCollect = rDDPPrimalDual.collect()

  def computeYPrimal() {
    val rDDYPrimal = computeRDDYPrimal()
    val yPrimalCollect = rDDYPrimal.collect()
    for (i <- 0 until numberPartitions) {
      for (j <- 0 until numberFeatures) {
        yPrimal(i, j) = yPrimalCollect(i)._2(j, 0)
      }
    }
  }

  def computeOutput(input: DenseVector[Double]) = {
    yPrimal(0, ::) * input
  }

  def computeQHessian() = {
    val rddQHessian = rDDPPrimalDual.mapPartitionsWithIndex((partitionID, iterator) => {
      iterator.map(row => 2.0 * row._2 * tmpZ(partitionID, ::).t)
    }, true)
    val qCollect = rddQHessian.collect()
    var qConcatenate = DenseMatrix(qCollect(0).copy)
    for (i <- 1 until qCollect.length) {
      qConcatenate = DenseMatrix.vertcat(qConcatenate, DenseMatrix(qCollect(i)))
    }
    qConcatenate
  }

  def computeRDDPPrimalDual() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        (partitionId, row._2 * row._2.t)
      })
    }, true).reduceByKey(_ + _).mapValues(_ + identity.map(_ * eta))
  }

  def computeRDDYPrimal() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val inputOutput = row._1 * row._2.t
        (partitionId, inputOutput)
      })
    }, true).reduceByKey(_ + _).map(v => {
      val partitionId = v._1
      (partitionId, inv(localPPrimalDualCollect(partitionId)._2) * (v._2 - 0.5 * qPrimalDual(partitionId, ::)).t)
    })

  }

}