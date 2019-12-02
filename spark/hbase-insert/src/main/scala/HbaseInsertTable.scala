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
//     Uncomment the below line if table is not present in HBase
      admin.createTable(tableDesc)
//    }

    val transactionRDD = spark.sparkContext.textFile(args(0))
      .filter(line => {
        val row = line.split(",")
        row(3).toDouble > 0 && row(1) != row(2)
      })

    transactionRDD.saveAsTextFile("op")

//    // REF: https://mapr.com/docs/52/Spark/SparkSQLandDataFrames.html
    // REF: https://stackoverflow.com/questions/42480770/wrting-to-hbase-maprdb-from-dataframe-in-spark-2
    transactionRDD.foreachPartition(rdd => {
      val config: Configuration = HBaseConfiguration.create()
      val connection: Connection = ConnectionFactory.createConnection(config)
      val table = connection.getTable(TableName.valueOf("ether_txn"))
      // REF: https://stackoverflow.com/questions/31356639/how-to-set-autoflush-false-in-hbase-table
      val mutator: BufferedMutator = connection.getBufferedMutator(TableName.valueOf("ether_txn"))

      rdd.foreach(line => {
        val row = line.split(",")
        val sender = row(2)
        val receiver = row(1)
        val index = row(4)

        val put = new Put(Bytes.add(Array(Bytes.toBytes(sender), Bytes.toBytes(index))))
        put.addColumn(Bytes.toBytes("id"), Bytes.toBytes("id"), Bytes.toBytes(row(0)))
        put.addColumn(Bytes.toBytes("sender"), Bytes.toBytes("sender"), Bytes.toBytes(sender))
        put.addColumn(Bytes.toBytes("receiver"), Bytes.toBytes("receiver"), Bytes.toBytes(receiver))
        put.addColumn(Bytes.toBytes("amount"), Bytes.toBytes("amount"), Bytes.toBytes(row(3)))
        put.addColumn(Bytes.toBytes("index"), Bytes.toBytes("index"), Bytes.toBytes(index))
        mutator.mutate(put)
      })

      table.close()
      mutator.flush()
      mutator.close()
//      connection.close()
    })
  }
}
