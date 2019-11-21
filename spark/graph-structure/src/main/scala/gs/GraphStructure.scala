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


    val transactionDF = spark.read.csv("input")
      .withColumnRenamed("_c0", "id")
      .withColumnRenamed("_c1", "receiver")
      .withColumnRenamed("_c2", "sender")
      .withColumnRenamed("_c3", "value")
      .withColumnRenamed("_c4", "index")

    transactionDF.show()

    println("FILTERED")
    transactionDF.filter("value != 0").show()
  }
}
