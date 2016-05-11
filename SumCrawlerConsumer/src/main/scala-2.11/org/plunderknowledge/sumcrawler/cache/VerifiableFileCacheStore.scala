package org.plunderknowledge.sumcrawler.cache

import javax.cache.Cache.Entry

import org.apache.ignite.cache.store.CacheStoreAdapter
import org.plunderknowledge.sumcrawler.model.{GpgKeyRing, Signature, SignatureType}
import scalikejdbc._
import org.plunderknowledge.sumcrawler.context._
import scalikejdbc.QueryDSL.{deleteFrom, insert, select}

/**
  * Created by greg on 5/10/16.
  */
class SignatureCacheStore extends CacheStoreAdapter[Long, Signature] {
  override def delete(key: Any): Unit = {
    withSQL {
      deleteFrom(Signature).where.eq(Signature.column.id, key)
    }.update.apply()
  }

  override def write(entry: Entry[_ <: Long, _ <: Signature]): Unit = {
    val m = Signature.column
    val n = GpgKeyRing.column
    val sig = entry.getValue
    withSQL {
      insert.into(Signature).namedValues(
        m.id -> entry.getKey,
        m.fullUrl -> sig.fullUrl,
        m.signature -> sig.signature,
        m.signatureTypeId -> sig.signatureTypeId,
        m.checkedDate -> sig.checkedDate,
        m.success -> sig.success
      )
    }.update.apply()
    sig.keyring.foreach {
      keyring =>
        withSQL {
          insert.into(GpgKeyRing).namedValues(n.keyring -> keyring.keyring)
        }
    }
  }

  override def load(key: Long): Signature = {
    val (s, st, k) = (Signature.syntax("s"), SignatureType.syntax("st"), GpgKeyRing.syntax("k"))
    val sig = withSQL {
      select.from(Signature as s).
        leftJoin(GpgKeyRing as k).on(s.keyringId, k.id).
        innerJoin(SignatureType as st).on(s.signatureTypeId, st.signatureType).
        where.eq(s.id, key)
    }.map(Signature(s, k, st) _).single.apply()
    sig.getOrElse {
      throw new Exception("signature not found")
    }
  }
}
