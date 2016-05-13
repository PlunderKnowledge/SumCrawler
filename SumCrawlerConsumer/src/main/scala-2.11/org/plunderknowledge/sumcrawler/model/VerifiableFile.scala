package org.plunderknowledge.sumcrawler.model

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL

import com.jsuereth.pgp.PublicKeyRing
import com.roundeights.hasher.Implicits.{stringToHasher, _}
import org.bouncycastle.openpgp.{PGPKeyRing, PGPPublicKeyRing, PGPSignature}
import org.joda.time.DateTime
import play.api.libs.json.Json
import scalikejdbc.QueryDSL
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

  implicit val urlTupleFormat = Json.format[VerifiableFile]

  val signatureTypeMap =
    Map[String, (DBSession => (VerifiableFile => Boolean))](
      ("md5" -> {implicit c: DBSession => x: VerifiableFile => verifyMd5(x)}),
      ("gpg" -> {implicit c: DBSession => x: VerifiableFile => verifyGpg(x)}))

  def verifyMd5(verifiableFile: VerifiableFile)(implicit session: DBSession): Boolean = {
    val fileUrl = new URL(verifiableFile.fileUrl)
    val fileStream = fileUrl.openStream()
    val fileSum = Stream.continually(fileStream.read).takeWhile(_ != -1).map(_.toByte).toArray.md5.hex
    val sum = verifiableFile.signature
    val isVerified = (fileSum == sum)
    val s = Signature.column
    applyUpdate {
      QueryDSL.insert.into(Signature).namedValues(
        s.fileUrl -> verifiableFile.fileUrl,
        s.signature -> verifiableFile.signature,
        s.signatureType -> verifiableFile.signatureType,
        s.checkedDate -> DateTime.now,
        s.success -> isVerified)
    }
    isVerified
  }
  def verifyGpg(verifiableFile: VerifiableFile)(implicit session: DBSession): Boolean = {
    verifiableFile.keyringUrl.map { keyRingUrl =>
      val keyRing = PublicKeyRing.load(new ByteArrayInputStream(Source.fromURL(keyRingUrl).map(_.toByte).toArray))
      val sigStream = new ByteArrayInputStream(verifiableFile.signature.getBytes)
      val fileStream = new ByteArrayInputStream(Source.fromURL(verifiableFile.fileUrl).map(_.toByte).toArray)
      val isVerified = keyRing.verifySignatureStreams(fileStream, sigStream)
      val id = sql"""insert into gpg_keyrings values (DEFAULT, ${keyRingUrl})""".update().apply()
      sql"""insert into signature values (DEFAULT, $id, ${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), ${isVerified})""".updateAndReturnGeneratedKey().apply()
      isVerified
    }.getOrElse {
      val id = sql"""insert into gpg_keyrings values (DEFAULT, DEFAULT)""".update().apply()
      sql"""insert into signature values (DEFAULT, DEFAULT, ${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), false)""".updateAndReturnGeneratedKey().apply()
      false
    }
  }
}