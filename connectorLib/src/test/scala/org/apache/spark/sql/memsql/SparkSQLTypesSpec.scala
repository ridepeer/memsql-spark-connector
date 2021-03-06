// scalastyle:off magic.number

package org.apache.spark.sql.memsql

import java.sql.{Date, Timestamp}

import com.memsql.spark.connector._
import com.memsql.spark.pushdown.{MemSQLPushdownStrategy, MemSQLPhysicalRDD}
import org.apache.spark.sql.Row
import org.apache.spark.sql.memsql.test.{SharedMemSQLContext, TestUtils}
import org.apache.spark.sql.types._
import org.scalatest.FlatSpec

class SparkSQLTypesSpec extends FlatSpec with SharedMemSQLContext {

  // Note that ByteType and BooleanType are both unused.
  //
  // Currently, we have SMALLINT, TINYINT, and BOOLEAN all turning into ShortTypes.
  // SMALLINT (32,768 to 32,767) makes sense as a ShortType.
  // TINYINT (-128 to 127) would make more sense as a ByteType, but it causes casting exceptions somewhere.
  // BOOLEAN aliases to TINYINT(1) in MySQL. TINYINT defaults to TINYINT(4).
  // JDBC cannot seem to tell the difference between a TINYINT(1) and a TINYINT(4), even with rsmd.getPrecision().
  val translatableSparkSQLTypes = Seq(
    (ShortType, Seq(7.toShort, 8.toShort, 256.toShort)),
    (IntegerType, Seq(1, 2, 65536)),
    (LongType, Seq(4L, 5L, 5000000000L)),
    (FloatType, Seq(2.8f, 3.1f, 4.2f)),
    (DoubleType, Seq(7.8, 9.1, 1.0000000000000001)),
    (StringType, Seq("hi", "there", "buddy")),
    (BinaryType, Seq("how".map(_.toByte).toArray, "are".map(_.toByte).toArray, "you".map(_.toByte).toArray)),
    (TimestampType, Seq(new Timestamp(90, 8, 23, 1, 1, 4, 0), new Timestamp(90, 8, 23, 1, 1, 5, 0), new Timestamp(90, 8, 23, 1, 1, 6, 0))),
    (DateType, Seq(new Date(90, 8, 23), new Date(90, 9, 23), new Date(90, 10, 23)))
  )

  it should "handle all SparkSQL types" in {
    val schema = StructType(translatableSparkSQLTypes.map(r =>
      StructField("val_" + r._1.toString, r._1, true))
    )

    val rows = (0 until 3).map(i =>
      Row.fromSeq(translatableSparkSQLTypes.map(_._2(i)))
    )

    val df1 = msc.createDataFrame(sc.parallelize(rows), schema)
    df1.saveToMemSQL("spark_sql_types")
    val df2 = msc.table("spark_sql_types").drop("memsql_insert_time")

    assert(df1.schema === df2.schema)

    assert(TestUtils.equalDFs(df1, df2))
  }
}
