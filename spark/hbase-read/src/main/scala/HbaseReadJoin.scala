package hbr

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{BufferedMutator, ColumnFamilyDescriptorBuilder, Connection, ConnectionFactory, Put, ResultScanner, Scan, TableDescriptorBuilder}
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object HbaseReadJoin {
  def main(args: Array[String]): Unit = {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nhbr.HbaseReadTable <input dir> <output dir>")
      System.exit(1)
    }

    val conf = new SparkConf()
    val spark = SparkSession.builder().appName("HBase Read & Join").config(conf).getOrCreate()

    // REF: https://stackoverflow.com/a/3870987
    val config: Configuration = HBaseConfiguration.create()
    config.set(TableInputFormat.INPUT_TABLE, "ether_txn")

    var transactionRDD = spark.sparkContext.textFile(args(0))
        .filter(line => {
          val row = line.split(",")
          row(3).toLong != 0
        })



    // REF: https://mapr.com/docs/52/Spark/SparkSQLandDataFrames.html
    // REF: https://stackoverflow.com/questions/42480770/wrting-to-hbase-maprdb-from-dataframe-in-spark-2
    var receiverKeyBroadcast = spark.sparkContext.broadcast(Array(0, 0, 1, 6, 11, 16))
    var joinIndexKeyBroadcast = spark.sparkContext.broadcast(Array(0, 0, 4, 9, 14, 19))

    for (i <- 2 to 5) {
      transactionRDD = transactionRDD.mapPartitions(rdd => {
        val config: Configuration = HBaseConfiguration.create()
        val connection: Connection = ConnectionFactory.createConnection(config)
        val table = connection.getTable(TableName.valueOf("ether_txn"))

        val joinedRDD = rdd.flatMap(line => {
           val row = line.split(",")
           if (receiverKeyBroadcast.value(i) < row.size){
           val receiver = row(receiverKeyBroadcast.value(i))
           val joinIndex = row(joinIndexKeyBroadcast.value(i)).toLong

           // REF: https://stackoverflow.com/a/21843961
           // REF: https://stackoverflow.com/q/21842469
           val prefix = Bytes.toBytes(receiver)
           val prefixFilter: PrefixFilter = new PrefixFilter(prefix)
           val scan = new Scan()
           scan.setFilter(prefixFilter)
           scan.setRowPrefixFilter(prefix)
           val resultScanner: ResultScanner = table.getScanner(scan)
           val result = resultScanner.iterator()
           var op = Seq[String]()
           while (result.hasNext) {
             val data = result.next()
             val id = Bytes.toString(data.getValue(Bytes.toBytes("id"), Bytes.toBytes("id")))
             val amount = Bytes.toString(data.getValue(Bytes.toBytes("amount"), Bytes.toBytes("amount")))
             val receiver = Bytes.toString(data.getValue(Bytes.toBytes("receiver"), Bytes.toBytes("receiver")))
             val sender = Bytes.toString(data.getValue(Bytes.toBytes("sender"), Bytes.toBytes("sender")))
             val index = Bytes.toString(data.getValue(Bytes.toBytes("index"), Bytes.toBytes("index")))

             if (index.toLong > joinIndex) {
               val joinedLine = line + "," + id + "," + receiver + "," + sender + "," + amount + "," + index
               op = op :+ joinedLine
             }
           }

           if (op.isEmpty)
             op = op :+ line

           op.map(joinedRow => joinedRow)
         } else {
           Array(line)
         }
        })

        table.close()
        //      connection.close()
        joinedRDD
      })
    }

    transactionRDD.saveAsTextFile(args(1))
  }
}
