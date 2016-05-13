package org.plunderknowledge.sumcrawler.model

import org.joda.time.DateTime
import scalikejdbc._

/**
  * Created by greg on 5/11/16.
  */
case class GpgKeyRing(id: Long, keyring: String)
object GpgKeyRing extends SQLSyntaxSupport[GpgKeyRing] {
  override val tableName = "gpg_keyring"
  def apply(o: SyntaxProvider[GpgKeyRing])(rs: WrappedResultSet): GpgKeyRing = apply(o.resultName)(rs)
  def apply(o: ResultName[GpgKeyRing])(rs: WrappedResultSet): GpgKeyRing = {
    GpgKeyRing(id = rs.long(o.id),
      keyring = rs.string(o.keyring))
  }
}

case class SignatureType(signatureType: String)
object SignatureType extends SQLSyntaxSupport[SignatureType] {
  override val tableName = "signature_type"
  def apply(o: SyntaxProvider[SignatureType])(rs: WrappedResultSet): SignatureType = apply(o.resultName)(rs)
  def apply(o: ResultName[SignatureType])(rs: WrappedResultSet): SignatureType = {
    SignatureType(signatureType = rs.string(o.signatureType))
  }
}

case class Signature(id: Long,
                     fileUrl: String,
                     signature: String,
                     checkedDate: DateTime,
                     success: Boolean,
                     signatureType: Option[SignatureType] = None,
                     keyring: Option[GpgKeyRing] = None)
object Signature extends SQLSyntaxSupport[Signature] {
  override val tableName = "signature"
  def apply(signatureSyntax: SyntaxProvider[Signature])(rs: WrappedResultSet): Signature = {
    apply(signatureSyntax.resultName)(rs)
  }
  def apply(signatureNames: ResultName[Signature])(rs: WrappedResultSet): Signature = {
    Signature(rs.long(signatureNames.id),
      rs.string(signatureNames.fileUrl),
      rs.string(signatureNames.signature),
      rs.jodaDateTime(signatureNames.checkedDate),
      rs.boolean(signatureNames.success))
  }
}
