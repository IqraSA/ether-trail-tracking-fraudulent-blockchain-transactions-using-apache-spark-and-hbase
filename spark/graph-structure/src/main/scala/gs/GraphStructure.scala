package gs

import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Column, SparkSession}
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

    var transactionDF = spark.read.csv("input")
      .withColumnRenamed("_c0", "id_1")
      .withColumnRenamed("_c1", "receiver_1")
      .withColumnRenamed("_c2", "sender_1")
      .withColumnRenamed("_c3", "value_1")
      .withColumnRenamed("_c4", "index_1")

    transactionDF = transactionDF.filter("value_1 != 0").select("id_1", "sender_1", "receiver_1", "value_1", "index_1").cache()
    var transactionDF2 = transactionDF
    var indexCols = Seq("index_1")

    for(i <- 2 to 5) {
      transactionDF2 = transactionDF2.as("df1")
        .join(transactionDF.as("df2"))
        .where(col("df1.receiver_"+(i-1)) === col("df2.sender_1") && col("df1.index_"+(i-1)) < col("df2.index_1"))
        .select(
          col("df1.*"),
          col("df2.id_1").as("id_"+i),
          col("df2.sender_1").as("sender_"+i),
          col("df2.receiver_1").as("receiver_"+i),
          col("df2.value_1").as("value_"+i),
          col("df2.index_1").as("index_"+i))

          indexCols = indexCols :+ "index_"+i

          // REF: https://stackoverflow.com/a/39818645
          val transactionDF3 = transactionDF2.select(transactionDF2.columns.filter(colName => !indexCols.contains(colName)).map(colName => new Column(colName)): _*)

          transactionDF3.write.csv(args(1) + "/" + i)
    }
  }
}
