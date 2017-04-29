package distributed_newton_star

import org.apache.spark._

object ClusterConfiguration {
  val conf = new SparkConf().setAppName("distributed-newton").setMaster("local[*]")
  val sc = new SparkContext(conf)
}