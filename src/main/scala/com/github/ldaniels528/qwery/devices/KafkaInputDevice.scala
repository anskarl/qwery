package com.github.ldaniels528.qwery.devices

import java.util.{Properties => JProperties}

import akka.actor.ActorRef
import com.github.ldaniels528.qwery.ops.Scope
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Kafka Input Device
  * @author lawrence.daniels@gmail.com
  */
case class KafkaInputDevice(topic: String, config: JProperties)
  extends InputDevice with AsyncInputDevice with RandomAccessInputDevice {
  private lazy val log = LoggerFactory.getLogger(getClass)
  private var consumer: Option[KafkaConsumer[String, Array[Byte]]] = None
  private var buffer: List[Record] = Nil
  private var once = true

  override def close(): Unit = {
    consumer.foreach(_.close())
    consumer = None
  }

  override def fastForward(partitions: Seq[Int]): Unit = {
    consumer.foreach(_.seekToEnd(partitions.map(new TopicPartition(topic, _)).asJava))
  }

  override def open(scope: Scope): Unit = {
    consumer = Option {
      val cons = new KafkaConsumer[String, Array[Byte]](config)
      cons.subscribe(List(topic).asJava)
      cons
    }
  }

  override def read(actor: ActorRef) {
    consumer.foreach(_.poll(1).asScala foreach { rec =>
      actor ! Record(rec.value(), rec.offset())
    })
  }

  override def read(): Option[Record] = {
    if(once) {
      once = !once
      val timeout = System.currentTimeMillis() + 120000L
      while(buffer.isEmpty && System.currentTimeMillis() < timeout) loadBuffer(5000L)
    }

    if (buffer.size < 100) {
      loadBuffer(5000L)
    }

    // read the next row
    buffer match {
      case Nil => None
      case row :: remaining => buffer = remaining; Option(row)
    }
  }

  override def rewind(partitions: Seq[Int]): Unit = {
    consumer.foreach(_.seekToBeginning(partitions.map(new TopicPartition(topic, _)).asJava))
  }

  override def seek(offset: Long, partition: Int = 0): Unit = {
    consumer.foreach(_.seek(new TopicPartition(topic, partition), offset))
  }

  private def loadBuffer(timeout: Long) = {
    log.info(s"Loading buffer... $timeout msec timeout")
    consumer.foreach(_.poll(timeout).asScala
      .foreach(rec => buffer = buffer ::: Record(rec.value, rec.offset, rec.partition) :: Nil))
    log.info(s"buffer: ${buffer.size}")
  }

}

/**
  * Kafka Input Device Companion
  * @author lawrence.daniels@gmail.com
  */
object KafkaInputDevice {

  def apply(topic: String,
            groupId: String,
            bootstrapServers: String,
            consumerProps: Option[JProperties] = None): KafkaInputDevice = {
    KafkaInputDevice(topic, {
      val props = new JProperties()
      props.put("group.id", groupId)
      props.put("bootstrap.servers", bootstrapServers)
      props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
      props.put("auto.offset.reset", "latest")
      props.put("enable.auto.commit", "false")
      consumerProps foreach props.putAll
      props
    })
  }
}