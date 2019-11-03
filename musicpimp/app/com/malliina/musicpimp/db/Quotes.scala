package com.malliina.musicpimp.db

import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

import com.malliina.play.auth.Token
import io.getquill.NamingStrategy
import io.getquill.context.Context
import io.getquill.idiom.Idiom

import scala.concurrent.duration.FiniteDuration

trait Quotes[I <: Idiom, N <: NamingStrategy] { this: Context[I, N] =>
  val foldersTable = quote(querySchema[DataFolder]("FOLDERS"))
  val tracksTable = quote(querySchema[DataTrack]("TRACKS"))
  val tokensTable = quote(querySchema[Token]("TOKENS"))

  implicit val instantDecoder = MappedEncoding[Date, Instant](d => d.toInstant)

  implicit val numericDuration: Numeric[FiniteDuration] = new Numeric[FiniteDuration] {
    override def plus(x: FiniteDuration, y: FiniteDuration): FiniteDuration = x.plus(y)
    override def minus(x: FiniteDuration, y: FiniteDuration): FiniteDuration = x.minus(y)
    override def times(x: FiniteDuration, y: FiniteDuration): FiniteDuration = x * y
    override def negate(x: FiniteDuration): FiniteDuration = -x
    override def fromInt(x: Int): FiniteDuration =
      FiniteDuration(Numeric.LongIsIntegral.fromInt(x), TimeUnit.NANOSECONDS)
    override def parseString(str: String): Option[FiniteDuration] = None
    override def toInt(x: FiniteDuration): Int = toDouble(x).toInt
    override def toLong(x: FiniteDuration): Long = x.toNanos
    override def toFloat(x: FiniteDuration): Float = toDouble(x).toFloat
    override def toDouble(x: FiniteDuration): Double = x.toUnit(TimeUnit.NANOSECONDS)
    override def compare(x: FiniteDuration, y: FiniteDuration): Int = x.compare(y)
  }
}
