package configuration

import org.apache.spark._

object ClusterConfiguration {
  val conf = new SparkConf().setAppName("distributed-newton").setMaster("local[*]")
      // .set("spark.network.timeout", "10000000")
      //.set("spark.executor.memory", "10g")
  val sc = new SparkContext(conf)
}

