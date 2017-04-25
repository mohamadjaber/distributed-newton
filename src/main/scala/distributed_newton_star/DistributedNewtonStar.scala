package distributed_newton_star

import org.apache.spark._
import org.apache.spark.rdd.RDD

import scala.math._
import breeze.linalg._
import breeze.numerics._

class DistributedNewtonStar(minNbPartitions: Int, eta: Double, inputFilePath: String) extends Serializable {
  val rddData = parseFile(inputFilePath, minNbPartitions).cache()
  val numberPartitions = rddData.getNumPartitions
  val numberFeatures = rddData.first()._2.size
  val lambdaDual = DenseMatrix.rand[Double](numberPartitions, numberFeatures)
  val laplacianMatrix = DenseMatrix.eye[Double](numberPartitions)
  val identity = DenseMatrix.eye[Double](numberFeatures)
  val qPrimalDual = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val yPrimal = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val tmpZ = new DenseMatrix[Double](numberPartitions, numberFeatures)

  setLaplacianMatrix()

  def parseFile(filePath: String, minPartitions: Int) = {
    ClusterConfiguration.sc.textFile(filePath, minPartitions).map(v => {
      val split = v.split("\\s+").map(_.toDouble)
      val len = split.length
      (split(len - 1), split.slice(0, len - 1))
    })
  }

  def learning(steps: Int) {
    println("Initial features")
    println(yPrimal)
    println("-------------------------------")
    for (iteration <- 0 until steps) {
      updateLambda()
      println("iteration " + iteration)
      println(yPrimal)
      println("-------------------------------")
    }
  }

  def updateLambda() {
    val rDDPPrimalDual = computeRDDPPrimalDual()
    val localPPrimalDualCollect = rDDPPrimalDual.collect()
    setQPrimalDual()
    val rDDYPrimal = computeRDDYPrimal(localPPrimalDualCollect)
    collectYPrimal(rDDYPrimal)
    setTmpZ()
    val rDDQ = computeRDDQ(rDDPPrimalDual)
    val qConcatenate = collectQ(rDDQ)
    val hessianDirection = computeHessianDirection(qConcatenate)
    updateLambdaDirection(hessianDirection)
  }

  def computeRDDPPrimalDual() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val input = new DenseMatrix(numberFeatures, 1, row._2)
        val inputT = new DenseMatrix(1, numberFeatures, row._2)
        (partitionId, input * inputT)
      })
    }, true).reduceByKey(_ + _).mapValues(_ + identity)
  }

  def setQPrimalDual() {
    for (indexPartition <- 0 until numberPartitions) {
      for (indexFeature <- 0 until numberFeatures) {
        val degree = if (indexPartition == 0) numberPartitions - 1 else 1
        val sumNeighborsLambda = if (indexPartition == 0) {
          var lambdaNeighbors = lambdaDual(::, indexFeature)
          lambdaNeighbors = lambdaNeighbors(1 until numberPartitions)
          lambdaNeighbors.reduce(_ + _)
        } else {
          lambdaDual(0, indexFeature)
        }
        qPrimalDual(indexPartition, indexFeature) = degree * lambdaDual(indexPartition, indexFeature) + sumNeighborsLambda
      }
    }
  }

  def computeRDDYPrimal(localPPrimalDualCollect: Array[(Int, DenseMatrix[Double])]) = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val inputOutput = new DenseMatrix(1, numberFeatures, row._2) * row._1
        (partitionId, inputOutput)
      })
    }, true).reduceByKey(_ + _).map(v => {
      val partitionId = v._1
      (partitionId, inv(localPPrimalDualCollect(partitionId)._2) * (v._2 - 0.5 * qPrimalDual(partitionId, ::)).t)
    })
  }

  def collectYPrimal(rDDYPrimal: RDD[(Int, DenseMatrix[Double])]) {
    val yPrimalCollect = rDDYPrimal.collect()
    for (i <- 0 until numberPartitions) {
      for (j <- 0 until numberFeatures) {
        yPrimal(i, j) = yPrimalCollect(i)._2(j, 0)
      }
    }
  }

  def setTmpZ() {
    for (i <- 0 until numberFeatures) {
      val laplacianYi = laplacianMatrix * yPrimal(::, i)
      val tmpZI = starSDDSolver(laplacianYi, laplacianMatrix)
      for (j <- 0 until numberPartitions) {
        tmpZ(j, i) = tmpZI(j)
      }
    }
  }

  def computeRDDQ(rDDPPrimalDual: RDD[(Int, DenseMatrix[Double])]) = {
    rDDPPrimalDual.mapPartitionsWithIndex((partitionID, iterator) => {
      iterator.map(row => 2.0 * row._2 * tmpZ(partitionID, ::).t)
    }, true)
  }

  def collectQ(rDDQ: RDD[DenseVector[Double]]) = {
    val qCollect = rDDQ.collect()
    var qConcatenate = DenseMatrix(qCollect(0).copy)
    for (i <- 1 until qCollect.length) {
      qConcatenate = DenseMatrix.vertcat(qConcatenate, DenseMatrix(qCollect(i)))
    }
    qConcatenate
  }

  def computeHessianDirection(qConcatenate: DenseMatrix[Double]) = {
    val hessianDirection = new DenseMatrix[Double](numberPartitions, numberFeatures)

    for (i <- 0 until numberFeatures) {
      val tmpHessian = starSDDSolver(qConcatenate(::, i), laplacianMatrix)
      for (j <- 0 until numberPartitions) {
        hessianDirection(j, i) = tmpHessian(j)
      }
    }
    hessianDirection
  }

  def setLaplacianMatrix() {
    laplacianMatrix(::, 0) := -1.0
    laplacianMatrix(0, ::) := -1.0
    laplacianMatrix(0, 0) = numberPartitions - 1
  }

  /**
   * TODO: to be parallelized
   */
  def updateLambdaDirection(hessianDirection: DenseMatrix[Double]) {
    for (i <- 0 until lambdaDual.rows) {
      for (j <- 0 until lambdaDual.cols) {
        lambdaDual(i, j) += hessianDirection(i, j) * eta
      }
    }
  }

  def starSDDSolver(outputVector: DenseVector[Double], laplacianMatrix: DenseMatrix[Double]) = {
    val tmp = 1.0 / Math.sqrt(numberPartitions * (numberPartitions - 1))
    val uNVector = DenseVector.ones[Double](numberPartitions).map(x => -tmp)
    uNVector(0) = uNVector(0) * tmp
    (laplacianMatrix * outputVector) +
      ((1.0 - numberPartitions * numberPartitions) / numberPartitions) * ((uNVector.t) * (uNVector * outputVector))
  }

}