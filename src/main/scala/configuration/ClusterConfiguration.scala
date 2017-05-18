package configuration

import org.apache.spark._

object ClusterConfiguration {
  val conf = new SparkConf().setAppName("distributed-newton").setMaster("local[*]") //.set("spark.executor.memory", "10g")
  val sc = new SparkContext(conf)
}

