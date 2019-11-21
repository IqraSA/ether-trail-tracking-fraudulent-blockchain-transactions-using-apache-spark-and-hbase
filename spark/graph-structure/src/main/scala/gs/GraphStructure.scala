package gs

import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object GraphStructure {
  def main(args: Array[String]): Unit = {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\ngs.GraphStructure <input dir> <output dir>")
      System.exit(1)
    }

    val conf = new SparkConf()
    val spark = SparkSession.builder().appName("Graph Structure").config(conf).getOrCreate()
    // IMP: Required for toDS, toDF functions
    import spark.implicits._
    val sc = spark.sparkContext


    var transactionDF = spark.read.csv("input")
      .withColumnRenamed("_c0", "id")
      .withColumnRenamed("_c1", "receiver")
      .withColumnRenamed("_c2", "sender")
      .withColumnRenamed("_c3", "value")
      .withColumnRenamed("_c4", "index")

    transactionDF = transactionDF.filter("value != 0").select("id", "sender", "receiver", "value", "index").cache()

    val transactionDF2 = transactionDF.as("df1")
      .join(transactionDF.as("df2"))
      .where($"df1.receiver" === $"df2.sender" && $"df1.index" < $"df2.index")
      .select(col("df1.id").as("id_1"),
        col("df1.sender").as("sender_1"),
        col("df1.receiver").as("receiver_1"),
        col("df1.value").as("value_1"),
        col("df2.id").as("id_2"),
        col("df2.sender").as("sender_2"),
        col("df2.receiver").as("receiver_2"),
        col("df2.value").as("value_2"),
        col("df2.index"))
  }
}
