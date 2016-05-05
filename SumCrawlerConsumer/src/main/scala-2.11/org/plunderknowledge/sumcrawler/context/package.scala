package org.plunderknowledge.sumcrawler

import akka.actor.{ActorSystem, Props}
import com.spingo.op_rabbit.RabbitControl
import scalikejdbc.AutoSession
import scalikejdbc.config.DBsWithEnv

/**
  * Created by gregrubino on 5/5/16.
  */
package object context {
  implicit val actorSystem = ActorSystem("sum-crawler")
  val rabbitControl = actorSystem.actorOf(Props[RabbitControl])

  DBsWithEnv("production").setupAll()
  implicit val session = AutoSession
}
