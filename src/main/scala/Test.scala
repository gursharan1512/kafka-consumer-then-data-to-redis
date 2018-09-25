import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import helper.{KafkaHelper, RedisHelper}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.InputDStream

object Test {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setMaster("local[*]").setAppName("KafkaReceiver")
    val ssc = new StreamingContext(conf, Seconds(5))

    val kafkaStream :InputDStream[ConsumerRecord[String,String]] = new KafkaHelper().readKafkaStream(ssc)

    kafkaStream.foreachRDD(rdd => {
      System.out.println("--- New RDD with "+rdd.partitions.size+" partitions and "+rdd.count()+" records")
      rdd.foreach(record => {
        println(record.value)
        val jedis = new RedisHelper().connecttoRedis()
        val parser: JsonParser = new JsonParser()
        val obj: JsonObject = parser.parse(record.value).getAsJsonObject()
        if (obj.has("v2")) {
          val obj2 = new RedisHelper().createJsonStringForRedis(obj,parser)
          jedis.lpush("obj2.get(SessionId).getAsString()+++++++++", obj2.toString)
        }
        else {
          println("No session ID... Data not stored in Redis")
        }
      }
      )
    })

    ssc.start
    ssc.awaitTermination
  }
}