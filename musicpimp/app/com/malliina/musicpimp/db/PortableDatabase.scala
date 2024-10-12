package com.malliina.musicpimp.db

import java.sql.{Timestamp, Types}
import java.time.Instant
import com.malliina.musicpimp.db.PortableDatabase.log
import io.getquill.*
import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import play.api.Logger

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

object PortableDatabase:
  private val log = Logger(getClass)

abstract class PortableDatabase[I <: SqlIdiom, N <: NamingStrategy](
  val naming: N,
  val ec: ExecutionContext
) extends JdbcContext[I, N]
  with NewMappings
  with Quotes[I, N]:
  implicit val ie: Encoder[Instant] = encoder(
    Types.TIMESTAMP,
    (idx, value, row) => row.setTimestamp(idx, new Timestamp(value.toEpochMilli))
  )
  implicit val id: Decoder[Instant] = decoder(row => idx => row.getTimestamp(idx).toInstant)

  def transactionally[T](name: String)(io: => T): Future[Result[T]] =
    performAsync(name)(io)

  def performAsync[T](name: String)(task: => T): Future[Result[T]] =
    wrapTask[T](name)(task)

//  def performAsync[T](name: String)(io: Quoted[T]): Future[Result[T]] =
//    Future(perform(name, io))(ec).recoverWith { case t =>
//      log.warn(s"Query '$name' failed.", t)
//      Future.failed(t)
//    }(ec)

  def wrapTask[T](name: String)(task: => T): Future[T] =
    Future(performTask(name, task))(ec).recoverWith { case t =>
      log.warn(s"Query '$name' failed.", t)
      Future.failed(t)
    }(ec)

//  private def perform[T](name: String, io: Quoted[T]): Result[T] =
//    val start = System.currentTimeMillis()
//    val result = run(io)
//    val end = System.currentTimeMillis()
//    val duration = (end - start).millis
//    val message = s"$name completed in $duration."
//    if duration > 500.millis then log.warn(message)
//    else if duration > 200.millis then log.info(message)
//    else log.debug(message)
//    result

  private def performTask[T](name: String, task: => T): T =
    val start = System.currentTimeMillis()
    val result = task
    val end = System.currentTimeMillis()
    val duration = (end - start).millis
    val message = s"$name completed in $duration."
    if duration > 500.millis then log.warn(message)
    else if duration > 200.millis then log.info(message)
    else log.debug(message)
    result

//  def first[T, E](io: IO[Seq[T], E], onEmpty: => String): IO[T, E] =
//    io.flatMap: ts =>
//      ts.headOption
//        .map: t =>
//          IO.successful(t)
//        .getOrElse:
//          IO.failed(new Exception(onEmpty))

  def fail(message: String): Nothing = PimpMySQLDatabase.fail(message)
