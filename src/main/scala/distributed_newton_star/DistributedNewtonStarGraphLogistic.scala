package distributed_newton_star

import org.apache.spark._
import org.apache.spark.rdd.RDD
import breeze.linalg._
import breeze.numerics._

class DistributedNewtonStarGraphLogistic(minNbPartitions: Int,
                                         eta: Double,
                                         stepSize: Double,
                                         alpha: Double,
                                         iterationLocalHessian: Int,
                                         inputFilePath: String)
    extends DistributedNewtonStarGraph(minNbPartitions, eta, stepSize, inputFilePath) {

  var rddHessianF: RDD[(Int, DenseMatrix[Double])] = _
  
  def computeYPrimal() {
    fillRandomMatrix(yPrimal)
    for (itr <- 0 until iterationLocalHessian) {
      val gradientF = computeRDDGradientF().collect()
      rddHessianF = computeRDDHessianF()
      if (itr == iterationLocalHessian - 1) {
        rddHessianF.cache() // cache to be used when computing q from z
      }
      val hessianInverseF = rddHessianF.mapValues(v => inv(v)).collect()

      for (i <- 0 until numberPartitions) {
        // TODO to parallelize
        val productHessianInverseGradient = hessianInverseF(i)._2 * gradientF(i)._2
        for (j <- 0 until numberFeatures) {
          yPrimal(i, j) = yPrimal(i, j) - (alpha * productHessianInverseGradient(j))
        }
      }
    }
  }

  def computeRDDGradientF() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val dotProduct = yPrimal(partitionId, ::).t dot row._2
        val innerSumRow = (1.0 / (
          1.0 + exp(-dotProduct)) + row._1) * row._2
        (partitionId, innerSumRow)
      })
    }, true).reduceByKey(_ + _).map(v => {
      val partitionId = v._1
      (partitionId, v._2 + (2.0 * eta * yPrimal(partitionId, ::).t) + qPrimalDual(partitionId, ::).t)
    })
  }

  def computeRDDHessianF() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val dotProduct = yPrimal(partitionId, ::).t dot row._2
        val innerSumRow = (1.0 / (
          pow(1.0 + exp(-dotProduct), 2))) * row._2 * row._2.t
        (partitionId, innerSumRow)
      })
    }, true).reduceByKey(_ + _).map(v => {
      val partitionId = v._1
      (partitionId, v._2 + (2.0 * eta * identity))
    })
  }

  def computeQHessian() = {
    val qCollect = rddHessianF.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(v => {
        v._2 * tmpZ(v._1, ::).t
      })
    }, true).collect()
    var qConcatenate = DenseMatrix(qCollect(0).copy)
    for (i <- 1 until qCollect.length) {
      qConcatenate = DenseMatrix.vertcat(qConcatenate, DenseMatrix(qCollect(i)))
    }
    qConcatenate
  }
}