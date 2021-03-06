package benchmarks
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import configuration.ClusterConfiguration._
import org.apache.spark.ml.classification.LogisticRegression


/**
 * @author ${user.name}
 */

@Deprecated
object AppMLLibLinear {
  def main(args: Array[String]) {
    // Load and parse the data
    val data = sc.textFile("input/linear_features_10")
    val parsedData = data.map { line =>
      val parts = line.split("\\s+").map(_.toDouble)
      val len = parts.length
      LabeledPoint(parts(len - 1), Vectors.dense(parts.slice(0, len - 1)))
      // LabeledPoint(parts(0).toDouble, Vectors.dense(parts(1).split(' ').map(_.toDouble)))
    }.cache()

    // Building the model
    val numIterations = 1000
    val stepSize = 10.9
    val model = LinearRegressionWithSGD.train(parsedData, numIterations, stepSize)

    // Evaluate model on training examples and compute training error
    val valuesAndPreds = parsedData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    
    println(model.weights)
    
    val MSE = valuesAndPreds.map { case (v, p) => math.pow((v - p), 2) }.reduce(_+_)
    println("training Mean Squared Error = " + MSE)
  }
}
