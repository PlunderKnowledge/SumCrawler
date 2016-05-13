package org.plunderknowledge.sumcrawler.model.test

import org.specs2.mutable.Specification
import org.plunderknowledge.sumcrawler.model.VerifiableFile

import scala.reflect.io.File
import scala.util.Random
import com.roundeights.hasher.Implicits._
import org.specs2.specification.BeforeAll
import scalikejdbc._
import scalikejdbc.config._
import scalikejdbc.specs2.mutable.AutoRollback

import org.flywaydb.core.Flyway


/**
  * Created by greg on 5/4/16.
  */
class VerifiableFileSpec extends Specification with BeforeAll {

  override def beforeAll(): Unit = {
    DBsWithEnv("test").setupAll()
    val flyway = new Flyway()
    flyway.setDataSource(ConnectionPool.dataSource('default))
    flyway.migrate()
  }

  sequential

  "Verifiable file should correctly identify correct sums" in new AutoRollback {
    val verifiable = VerifiableFile(VerifiableFileSpec.correctFileUrl, VerifiableFileSpec.correctFileSum, "md5", None)
    verifiable.verify() must beTrue
    sql"""select count(*) as success_count from signature
          where success = true""".map(rs => rs.int("success_count")).single.apply() must beEqualTo(Some(1))
  }
}

object VerifiableFileSpec {

  def kestrel[A](x: A)(f: A => Unit): A = { f(x); x }

  def writeFile(bytes: Array[Byte]): String = {
    val f = File.makeTemp()
    f.outputStream().write(bytes)
    f.toAbsolute.path
  }

  val correctFileBytes = kestrel(Array.fill[Byte](200)(0))(Random.nextBytes)
  val tempFileName = writeFile(correctFileBytes)
  val correctFileSum = correctFileBytes.md5.hex
  val correctFileUrl = s"file://${tempFileName}"

}
