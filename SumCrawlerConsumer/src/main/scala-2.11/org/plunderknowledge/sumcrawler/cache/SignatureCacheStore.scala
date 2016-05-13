package org.plunderknowledge.sumcrawler.cache

import javax.cache.Cache.Entry

import org.apache.ignite.cache.store.CacheStoreAdapter
import org.plunderknowledge.sumcrawler.model.{GpgKeyRing, Signature, SignatureType}
import scalikejdbc._
import org.plunderknowledge.sumcrawler.context._
import scalikejdbc.QueryDSL

/**
  * Created by greg on 5/10/16.
  */
class SignatureCacheStore extends CacheStoreAdapter[Long, Signature] {
  override def delete(key: Any): Unit = {
    withSQL {
      QueryDSL.deleteFrom(Signature).where.eq(Signature.column.id, key)
    }.update.apply()
  }

  override def write(entry: Entry[_ <: Long, _ <: Signature]): Unit = {
    val m = Signature.column
    val n = GpgKeyRing.column
    val sig = entry.getValue
    applyUpdate {
      QueryDSL.insert.into(Signature).namedValues(
        m.id -> entry.getKey,
        m.fileUrl -> sig.fileUrl,
        m.signature -> sig.signature,
        m.checkedDate -> sig.checkedDate,
        m.success -> sig.success
      )
    }
    sig.keyring.foreach {
      keyring =>
        applyUpdate {
          QueryDSL.insert.into(GpgKeyRing).namedValues(n.keyring -> keyring.keyring)
        }
    }
  }

  override def load(key: Long): Signature = {
    val (s, st, k) = (Signature.syntax("s"), SignatureType.syntax("st"), GpgKeyRing.syntax("k"))
    val sig = withSQL {
      QueryDSL.select.from(Signature as s).
        leftJoin(GpgKeyRing as k).on(s.column("keyringId"), k.id).
        innerJoin(SignatureType as st).on(s.signatureType, st.signatureType).
        where.eq(s.id, key)
    }.map(rs => Signature(s)(rs)).single.apply()
    sig.getOrElse {
      throw new Exception("signature not found")
    }
  }
}
