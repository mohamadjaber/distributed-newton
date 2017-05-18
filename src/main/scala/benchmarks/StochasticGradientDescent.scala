package benchmarks

import configuration.ClusterConfiguration._
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.mllib.regression.LinearRegressionWithSGD

class StochasticGradientDescent(minNbPartitions: Int,
    eta: Double,
    stepSize: Double,
    inputFilePath: String) {

  val data = sc.textFile(inputFilePath)
  val rddData = data.map { line =>
    val parts = line.split("\\s+").map(_.toDouble)
    val len = parts.length
    LabeledPoint(parts(len - 1), Vectors.dense(parts.slice(0, len - 1)))
  }.repartition(minNbPartitions).cache()

  def learning(steps: Int) {
    val model = LinearRegressionWithSGD.train(rddData, steps, stepSize)
   
    // Evaluate model on training examples and compute training error
    val valuesAndPreds = rddData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }

    val MSE = valuesAndPreds.map { case (v, p) => math.pow((v - p), 2) }.reduce(_ + _)
    println("training Mean Squared Error = " + MSE)
  }
}