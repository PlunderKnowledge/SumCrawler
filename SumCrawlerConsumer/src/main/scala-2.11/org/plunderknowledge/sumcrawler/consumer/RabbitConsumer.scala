package org.plunderknowledge.sumcrawler.consumer

import javax.cache.configuration.FactoryBuilder

import com.roundeights.hasher.Algo
import com.spingo.op_rabbit.Subscription
import com.spingo.op_rabbit.PlayJsonSupport._
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.{Ignite, Ignition}
import org.plunderknowledge.sumcrawler.cache.SignatureCacheStore
import org.plunderknowledge.sumcrawler.model.{Signature, VerifiableFile}
import org.plunderknowledge.sumcrawler.context._
import scalikejdbc._
import scalikejdbc.QueryDSL._

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by greg on 4/30/16.
  */
object RabbitConsumer extends App {

  val igniteConfig = "ignite-config.xml"
  val ignite = Ignition.start("ignite-config.xml")
  val cacheCfg = new CacheConfiguration[Long, Signature]()
  cacheCfg.setCacheMode(CacheMode.PARTITIONED)
  cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(classOf[SignatureCacheStore]))
  cacheCfg.setReadThrough(true)
  cacheCfg.setWriteThrough(true)
  val signatureCache = ignite.getOrCreateCache[Long, Signature]("signature-cache")

  val subscriptionRef = Subscription.run(rabbitControl) {
    import com.spingo.op_rabbit.Directives._
    channel(qos = 10) {
      consume(topic(queue("sum-crawler-queue", durable = true), List("verifiable-files"))) {
        (body(as[VerifiableFile])) {
          file =>
            DB localTx { implicit session => file.verify }
            ack
        }
      }
    }
  }

}
