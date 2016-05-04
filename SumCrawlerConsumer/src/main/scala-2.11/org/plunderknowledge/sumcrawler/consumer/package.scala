package org.plunderknowledge.sumcrawler

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.spingo.op_rabbit.RabbitControl
import com.typesafe.config.{Config, ConfigFactory}
import org.plunderknowledge.sumcrawler.model.VerifiableFile
import play.api.libs.json.Json
import scalikejdbc.{AutoSession, ConnectionPool}

/**
  * Created by greg on 4/30/16.
  */
package object consumer {

  val config = ConfigFactory.parseFile(new File(this.getClass.getClassLoader.getResource("application.conf").getFile))

  implicit val urlTupleFormat = Json.format[VerifiableFile]
  implicit val actorSystem = ActorSystem("sum-crawler")
  val rabbitControl = actorSystem.actorOf(Props[RabbitControl])

  Class.forName("org.postgresql.Driver")
  ConnectionPool.singleton(
    config.getString("postgres.url"), config.getString("postgres.user"), config.getString("postgres.password"))

  implicit val session = AutoSession

}
