package distributed_newton_star

import org.apache.spark._
import org.apache.spark.rdd.RDD

import breeze.linalg._
import breeze.numerics._
import configuration.ClusterConfiguration

abstract class DistributedNewtonStarGraph(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) extends Serializable {

  val rddData = parseFile(inputFilePath).repartition(minNbPartitions).cache()
  val numberPartitions = rddData.getNumPartitions
  val numberFeatures = rddData.first()._2.size
  val lambdaDual = DenseMatrix.rand[Double](numberPartitions, numberFeatures)
  val laplacianMatrix = DenseMatrix.eye[Double](numberPartitions)
  val identity = DenseMatrix.eye[Double](numberFeatures)
  val qPrimalDual = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val yPrimal = DenseMatrix.zeros[Double](numberPartitions, numberFeatures)
  val tmpZ = new DenseMatrix[Double](numberPartitions, numberFeatures)

  setLaplacianMatrix()

  // abstract methods
  def computeYPrimal()
  def computeQHessian(): DenseMatrix[Double]
  def computeOutput(input: DenseVector[Double]): Double
  def computeError(): Double
  def debugGradient(): Double

  def updateLambda() {
    setQPrimalDual()
    computeYPrimal()
    computeTmpZ()
    val qConcatenate = computeQHessian()
    onePerpProjection(qConcatenate)
    val hessianDirection = computeHessianDirection(qConcatenate)
    updateLambdaDirection(hessianDirection)
  }

  def parseFile(filePath: String) = {
    ClusterConfiguration.sc.textFile(filePath).map(v => {
      val split = v.split("\\s+").map(_.toDouble)
      val len = split.length
      (split(len - 1), DenseVector(split.slice(0, len - 1)))
    })
  }

  def learning(steps: Int) {
    for (iteration <- 0 until steps) {
      if ((iteration != 0) && (iteration % 10 == 0)) println("done " + iteration + " iterations")
      updateLambda()
    }
  }

  def learningBench(steps: Int) = {
    val bench = Array.ofDim[Double](steps, 3) // this array holds gradient, error and consensus error
    for (iteration <- 0 until steps) {
      if ((iteration != 0) && (iteration % 10 == 0)) println("done " + iteration + " iterations")
      updateLambda()
      bench(iteration)(0) = debugGradient()
      bench(iteration)(1) = computeError()
      bench(iteration)(2) = computeConsesusError(yPrimal)
    }
    bench
  }

  def onePerpProjection(matrix: DenseMatrix[Double]) {
    for (j <- 0 until matrix.cols) {
      val sumI = sum(matrix(::, j))
      for (i <- 0 until matrix.rows) {
        matrix(i, j) -= sumI / matrix.rows
      }
    }
  }

  def setQPrimalDual() {
    for (indexPartition <- 0 until numberPartitions) {
      for (indexFeature <- 0 until numberFeatures) {
        val degree = if (indexPartition == 0) numberPartitions - 1 else 1
        val sumNeighborsLambda = if (indexPartition == 0) {
          sum(lambdaDual(::, indexFeature)) - lambdaDual(0, indexFeature)
        } else {
          lambdaDual(0, indexFeature)
        }
        qPrimalDual(indexPartition, indexFeature) = degree * lambdaDual(indexPartition, indexFeature) - sumNeighborsLambda
      }
    }
  }

  def localError(vector: DenseVector[Double]) = {
    val averageJ = sum(vector) / vector.length
    var maximumValue = abs(vector(0) - averageJ)
    for (i <- 1 until vector.length) {
      val v = abs(vector(i) - averageJ)
      maximumValue = max(v, maximumValue)
    }
    maximumValue
  }

  def computeConsesusError(matrix: DenseMatrix[Double]) = {
    var error = Double.NegativeInfinity
    for (i <- 0 until matrix.cols) {
      error = max(error, localError(matrix(::, i)))
    }
    error
  }

  def computeTmpZ() {
    for (i <- 0 until numberFeatures) {
      val laplacianYi = laplacianMatrix * yPrimal(::, i)
      val tmpZI = starSDDSolver(laplacianYi, laplacianMatrix)
      for (j <- 0 until numberPartitions) {
        tmpZ(j, i) = tmpZI(j)
      }
    }
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
        lambdaDual(i, j) += hessianDirection(i, j) * stepSize
      }
    }
  }

  def starSDDSolver(outputVector: DenseVector[Double], laplacianMatrix: DenseMatrix[Double]) = {
    val tmp = 1.0 / sqrt(numberPartitions * (numberPartitions - 1))
    val uNVector = DenseVector.ones[Double](numberPartitions).map(x => -tmp)
    uNVector(0) = uNVector(0) * (1 - numberPartitions)
    (laplacianMatrix * outputVector) +
      ((1.0 - numberPartitions * numberPartitions) / numberPartitions) * ((uNVector.t * outputVector) * (uNVector))
  }

  def fillRandomMatrix(matrix: DenseMatrix[Double]) {
    for (i <- 0 until matrix.rows) {
      for (j <- 0 until matrix.cols) {
        matrix(i, j) = Math.random()
      }
    }
  }

}