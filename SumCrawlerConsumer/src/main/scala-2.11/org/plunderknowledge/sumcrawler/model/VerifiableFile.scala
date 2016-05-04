package org.plunderknowledge.sumcrawler.model

import java.io.{ByteArrayInputStream, InputStream}

import com.jsuereth.pgp.PublicKeyRing
import com.roundeights.hasher.Algo
import org.bouncycastle.openpgp.{PGPKeyRing, PGPPublicKeyRing, PGPSignature}
import scalikejdbc._

import scala.io.Source

/**
  * Created by greg on 4/30/16.
  */
case class VerifiableFile(fileUrl: String, signature: String, signatureType: String, keyringUrl: Option[String]) {

  def verify()(implicit session: DBSession): Boolean = {
    VerifiableFile.signatureTypeMap(signatureType)(session)(this)
  }
}

object VerifiableFile {

  val signatureTypeMap =
    Map[String, (DBSession => (VerifiableFile => Boolean))](
      ("md5" -> {implicit c: DBSession => x: VerifiableFile => verifyMd5(x)}),
      ("gpg" -> {implicit c: DBSession => x: VerifiableFile => verifyGpg(x)}))

  def verifyMd5(verifiableFile: VerifiableFile)(implicit session: DBSession): Boolean = {
    val fileSum = Algo.md5.tap(Source.fromURL(verifiableFile.fileUrl)).mkString
    val sum = verifiableFile.signature
    val isVerified = (fileSum == sum)
    sql"""insert into signature values (${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), ${isVerified})""".update()
    isVerified
  }
  def verifyGpg(verifiableFile: VerifiableFile)(implicit session: DBSession): Boolean = {
    verifiableFile.keyringUrl.map { keyRingUrl =>
      val keyRing = PublicKeyRing.load(new ByteArrayInputStream(Source.fromURL(keyRingUrl).map(_.toByte).toArray))
      val sigStream = new ByteArrayInputStream(verifiableFile.signature.getBytes)
      val fileStream = new ByteArrayInputStream(Source.fromURL(verifiableFile.fileUrl).map(_.toByte).toArray)
      val isVerified = keyRing.verifySignatureStreams(fileStream, sigStream)
      val id = sql"""insert into signature values (${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), ${isVerified})""".updateAndReturnGeneratedKey()
      sql"""insert into gpg_keyrings values (${id}, ${keyRingUrl})"""
      isVerified
    }.getOrElse {
      val id = sql"""insert into signature values (${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), false)""".updateAndReturnGeneratedKey()
      sql"""insert into gpg_keyrings values (${id}, '')"""
      false
    }
  }
}