package distributed_newton_star

import org.apache.spark._
import org.apache.spark.rdd.RDD

import scala.math._
import breeze.linalg._
import breeze.numerics._

class DistributedNewtonStarLogistic(minNbPartitions: Int, eta: Double, inputFilePath: String) extends Serializable {
  val rddData = parseFile(inputFilePath, minNbPartitions).cache()
  val numberPartitions = rddData.getNumPartitions
  val numberFeatures = rddData.first()._2.size
  val lambdaDual = DenseMatrix.rand[Double](numberPartitions, numberFeatures)
  val laplacianMatrix = DenseMatrix.eye[Double](numberPartitions)
  val identity = DenseMatrix.eye[Double](numberFeatures)
  val qPrimalDual = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val yPrimal = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val tmpZ = new DenseMatrix[Double](numberPartitions, numberFeatures)

  val iterationLocalHessian = 4
  val alpha = 0.1

  setLaplacianMatrix()
  // val rDDPPrimalDual = computeRDDPPrimalDual()
  // val localPPrimalDualCollect = rDDPPrimalDual.collect()

  def parseFile(filePath: String, minPartitions: Int) = {
    ClusterConfiguration.sc.textFile(filePath, minPartitions).map(v => {
      val split = v.split("\\s+").map(_.toDouble)
      val len = split.length
      (split(len - 1), DenseVector(split.slice(0, len - 1)))
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
      println("Consensur Error: " + computeConsesusError(yPrimal))
      println("-------------------------------")
    }
  }

  def updateLambda() {
    setQPrimalDual()
    val rddHessianF = computeYPrimal()
    setTmpZ()
    val qConcatenate = computeQ(rddHessianF)
    onePerpProjection(qConcatenate)
    val hessianDirection = computeHessianDirection(qConcatenate)
    updateLambdaDirection(hessianDirection)
  }

  def onePerpProjection(matrix: DenseMatrix[Double]) {
    for (j <- 0 until matrix.cols) {
      val sumI = sum(matrix(::, j))
      for (i <- 0 until matrix.rows) {
        matrix(i, j) -= sumI / matrix.rows
      }
    }
  }

  def computeRDDPPrimalDual() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        (partitionId, row._2 * row._2.t)
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

  def localError(vector: DenseVector[Double]) = {
    val averageJ = sum(vector) / vector.length
    var maximumValue = Math.abs(vector(0) - averageJ)
    for (i <- 1 until vector.length) {
      val v = Math.abs(vector(i) - averageJ)
      maximumValue = Math.max(v, maximumValue)
    }
    maximumValue
  }

  def computeConsesusError(matrix: DenseMatrix[Double]) = {
    var error = Double.NegativeInfinity
    for (i <- 0 until matrix.cols) {
      error = Math.max(error, localError(matrix(::, i)))
    }
    error
  }

  def computeRDDGradientF() = {
    rddData.mapPartitionsWithIndex((partitionId, iterator) => {
      iterator.map(row => {
        val dotProduct = yPrimal(partitionId, ::).t dot row._2
        val innerSumRow = (1.0 / (
          1.0 + scala.math.exp(-dotProduct)) + row._1) * row._2
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
          scala.math.pow(1.0 + scala.math.exp(-dotProduct), 2))) * row._2 * row._2.t
        (partitionId, innerSumRow)
      })
    }, true).reduceByKey(_ + _).map(v => {
      val partitionId = v._1
      (partitionId, v._2 + (2.0 * eta * identity))
    })
  }

  def computeYPrimal() = {
    fillRandomMatrix(yPrimal)
    var rddHessianF: RDD[(Int, DenseMatrix[Double])] = null
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
    rddHessianF
  }

  def fillRandomMatrix(matrix: DenseMatrix[Double]) {
    for (i <- 0 until matrix.rows) {
      for (j <- 0 until matrix.cols) {
        matrix(i, j) = Math.random()
      }
    }
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

  def computeQ(rddHessianF: RDD[(Int, DenseMatrix[Double])]) = {
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