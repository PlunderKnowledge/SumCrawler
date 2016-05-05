package org.plunderknowledge.sumcrawler.consumer

import com.roundeights.hasher.Algo
import com.spingo.op_rabbit.Subscription
import com.spingo.op_rabbit.PlayJsonSupport._
import org.plunderknowledge.sumcrawler.model.VerifiableFile
import org.plunderknowledge.sumcrawler.context._
import scalikejdbc._
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by greg on 4/30/16.
  */
object RabbitConsumer extends App {

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
