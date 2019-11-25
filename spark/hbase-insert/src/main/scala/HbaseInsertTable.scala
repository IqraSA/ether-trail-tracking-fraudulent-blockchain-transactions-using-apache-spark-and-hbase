package hbi

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{BufferedMutator, ColumnFamilyDescriptorBuilder, Connection, ConnectionFactory, HBaseAdmin, HTable, Put, Table, TableDescriptorBuilder}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.hadoop.hbase.spark.datasources.HBaseTableCatalog
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapreduce.Job
import org.apache.log4j.LogManager
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DataTypes, LongType, Metadata, StringType, StructField, StructType}
//import org.apache.spark.sql.execution.datasources.hbase._


object HbaseInsertTable {

  def main(args: Array[String]): Unit = {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 1) {
      logger.error("Usage:\nhbi.HbaseInserTable <input dir>")
      System.exit(1)
    }

    val conf = new SparkConf()
    val spark = SparkSession.builder().appName("HBase Table Insert").config(conf).getOrCreate()

    // REF: https://stackoverflow.com/a/3870987
    val config: Configuration = HBaseConfiguration.create()
    config.set(TableInputFormat.INPUT_TABLE, "ether_txn")
//    config.set("zookeeper.znode.parent","/hbase")

    // REF: https://community.cloudera.com/t5/Support-Questions/Read-HBase-Table-by-using-Spark-Scala/td-p/164293
    val connection: Connection = ConnectionFactory.createConnection(config)
    val admin = connection.getAdmin
    val tableName = TableName.valueOf("ether_txn")


    // REF: https://issues.apache.org/jira/browse/HBASE-19145
//    if(!admin.isTableAvailable(tableName)) {
      // REF: https://stackoverflow.com/a/53507285
      val tableDesc = TableDescriptorBuilder
          .newBuilder(tableName)
          .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder("id".getBytes).build())
          .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder("sender".getBytes).build())
          .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder("receiver".getBytes).build())
          .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder("amount".getBytes).build())
          .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder("index".getBytes).build())
          .build()
//      admin.createTable(tableDesc)
//    }


    // REF: https://stackoverflow.com/a/34388744
    var transactionDF = spark.read.csv(args(0))
      .withColumnRenamed("_c0", "id")
      .withColumnRenamed("_c1", "receiver")
      .withColumnRenamed("_c2", "sender")
      .withColumnRenamed("_c3", "value")
      .withColumnRenamed("_c4", "index")

//    // REF: https://mapr.com/docs/52/Spark/SparkSQLandDataFrames.html
    // REF: https://stackoverflow.com/questions/42480770/wrting-to-hbase-maprdb-from-dataframe-in-spark-2
    transactionDF.rdd.foreachPartition(rdd => {
      val config: Configuration = HBaseConfiguration.create()
      val connection: Connection = ConnectionFactory.createConnection(config)
      val table = connection.getTable(TableName.valueOf("ether_txn"))
      // REF: https://stackoverflow.com/questions/31356639/how-to-set-autoflush-false-in-hbase-table
      val mutator: BufferedMutator = connection.getBufferedMutator(TableName.valueOf("ether_txn"))

      rdd.foreach(row => {
        val sender = row(2).toString
        val put = new Put(Bytes.toBytes(sender))

        put.addColumn(Bytes.toBytes("id"), Bytes.toBytes("id"), Bytes.toBytes(row(0).toString))
        put.addColumn(Bytes.toBytes("receiver"), Bytes.toBytes("receiver"), Bytes.toBytes(row(1).toString))
        put.addColumn(Bytes.toBytes("amount"), Bytes.toBytes("amount"), Bytes.toBytes(row(3).toString.toLong))
        put.addColumn(Bytes.toBytes("index"), Bytes.toBytes("index"), Bytes.toBytes(row(4).toString.toLong))
        mutator.mutate(put)
      })
//      (new ImmutableBytesWritable(Bytes.toBytes(sender)), put)
      table.close()
      mutator.flush()
      mutator.close()
    })
  }
}
