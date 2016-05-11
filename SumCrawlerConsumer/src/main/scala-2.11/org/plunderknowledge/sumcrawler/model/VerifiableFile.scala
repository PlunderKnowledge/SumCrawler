package org.plunderknowledge.sumcrawler.model

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL

import com.jsuereth.pgp.PublicKeyRing
import com.roundeights.hasher.Implicits.{stringToHasher, _}
import org.bouncycastle.openpgp.{PGPKeyRing, PGPPublicKeyRing, PGPSignature}
import org.joda.time.DateTime
import play.api.libs.json.Json
import scalikejdbc._

import scala.io.Source

case class GpgKeyRing(id: Long, keyring: String)
object GpgKeyRing extends SQLSyntaxSupport[GpgKeyRing] {
  override val tableName = "gpg_keyring"
  def apply(keyringTypeNames: ResultName[GpgKeyRing])(rs: WrappedResultSet): GpgKeyRing = {
    GpgKeyRing(id = rs.long(keyringTypeNames.id),
      keyring = rs.string(keyringTypeNames.keyring))
  }
}

case class SignatureType(signatureType: String)
object SignatureType extends SQLSyntaxSupport[SignatureType] {
  override val tableName = "signature_type"
  def apply(signatureTypeNames: ResultName[SignatureType])(rs: WrappedResultSet): SignatureType = {
    SignatureType(signatureType = rs.string(signatureTypeNames.signatureType))
  }
}

case class Signature(id: Long,
                     keyringId: Option[Long],
                     fullUrl: String,
                     signature: String,
                     signatureType: SignatureType,
                     signatureTypeId: String,
                     checkedDate: DateTime,
                     success: Boolean,
                     keyring: Option[GpgKeyRing])
object Signature extends SQLSyntaxSupport[Signature] {
  override val tableName = "signature"
  def apply(signatureSyntax: SyntaxProvider[Signature],
            signatureType: SyntaxProvider[SignatureType])(rs: WrappedResultSet): Signature = {
    apply(signatureSyntax.resultName, SignatureType(signatureType.resultName)(rs))(rs)
  }
  def apply(signatureNames: ResultName[Signature],
            signatureType: SignatureType)(rs: WrappedResultSet): Signature = {
    Signature(rs.long(signatureNames.id),
      rs.longOpt(signatureNames.keyringId),
      rs.string(signatureNames.fullUrl),
      rs.string(signatureNames.signature),
      signatureType,
      rs.string(signatureNames.signatureTypeId),
      rs.jodaDateTime(signatureNames.checkedDate),
      rs.boolean(signatureNames.success),
      None)
  }
  def apply(signatureSyntax: SyntaxProvider[Signature],
            gpgKeyRingSyntax: SyntaxProvider[GpgKeyRing],
            signatureTypeSyntax: SyntaxProvider[SignatureType])(rs: WrappedResultSet): Signature = {
    apply(signatureSyntax, signatureTypeSyntax)(rs).copy(keyring = Some(GpgKeyRing(gpgKeyRingSyntax.resultName)(rs)))
  }
}

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
    sql"""insert into signature values (DEFAULT, ${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), ${isVerified})""".update().apply()
    isVerified
  }
  def verifyGpg(verifiableFile: VerifiableFile)(implicit session: DBSession): Boolean = {
    verifiableFile.keyringUrl.map { keyRingUrl =>
      val keyRing = PublicKeyRing.load(new ByteArrayInputStream(Source.fromURL(keyRingUrl).map(_.toByte).toArray))
      val sigStream = new ByteArrayInputStream(verifiableFile.signature.getBytes)
      val fileStream = new ByteArrayInputStream(Source.fromURL(verifiableFile.fileUrl).map(_.toByte).toArray)
      val isVerified = keyRing.verifySignatureStreams(fileStream, sigStream)
      val id = sql"""insert into signature values (DEFAULT, ${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), ${isVerified})""".updateAndReturnGeneratedKey().apply()
      sql"""insert into gpg_keyrings (file_url, signature, signature_type, checked_date, success) values (${id}, ${keyRingUrl})""".update().apply()
      isVerified
    }.getOrElse {
      val id = sql"""insert into signature values (DEFAULT, ${verifiableFile.fileUrl}, ${verifiableFile.signature}, ${verifiableFile.signatureType}, NOW(), false)""".updateAndReturnGeneratedKey().apply()
      sql"""insert into gpg_keyrings values (${id}, DEFAULT)""".update().apply()
      false
    }
  }
}