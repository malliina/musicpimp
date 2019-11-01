package com.malliina.musicpimp.db
import java.sql.{Timestamp, Types}
import java.time.Instant

import akka.actor.ActorSystem
import com.malliina.musicpimp.db.PimpDatabase.log
import com.zaxxer.hikari.HikariDataSource
import io.getquill._
import org.flywaydb.core.Flyway
import play.api.Logger

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class PimpMySQL(ds: HikariDataSource, ec: ExecutionContext, isMariaDb: Boolean)
  extends PimpDatabase(
    ds,
    CompositeNamingStrategy2(SnakeCase, UpperCase),
    ec,
    isMariaDb
  )

object PimpDatabase {
  private val log = Logger(getClass)

  def withMigrations(as: ActorSystem, conf: Conf): PimpMySQL = {
    val flyway =
      Flyway.configure
        .dataSource(conf.url, conf.user, conf.pass)
        //        .table("flyway_schema_history2")
        .load()
    flyway.migrate()
    apply(as, conf)
  }

  private def apply(as: ActorSystem, dbConf: Conf): PimpMySQL = {
    val pool = as.dispatchers.lookup("contexts.database")
    apply(Conf.dataSource(dbConf), pool, dbConf.isMariaDb)
  }

  private def apply(ds: HikariDataSource, ec: ExecutionContext, isMariaDb: Boolean): PimpMySQL = {
    new PimpMySQL(ds, ec, isMariaDb)
  }

  def fail(message: String): Nothing = throw new Exception(message)
}

class PimpDatabase[N <: NamingStrategy](
  val ds: HikariDataSource,
  naming: N,
  val ec: ExecutionContext,
  isMariaDb: Boolean
) extends MysqlJdbcContext(naming, ds)
  with NewMappings
  with Quotes[MySQLDialect, N] {
  implicit val ie: Encoder[Instant] = encoder(
    Types.TIMESTAMP,
    (idx, value, row) => row.setTimestamp(idx, new Timestamp(value.toEpochMilli))
  )

  def transactionally[T](name: String)(io: IO[T, _]): Future[Result[T]] =
    performAsync(name)(io.transactional)

  def performAsync[T](name: String)(io: IO[T, _]): Future[Result[T]] = Future(perform(name, io))(ec)

  def perform[T](name: String, io: IO[T, _]): Result[T] = {
    val start = System.currentTimeMillis()
    val result = performIO(io)
    val end = System.currentTimeMillis()
    val duration = (end - start).millis
    val message = s"$name completed in $duration."
    if (duration > 500.millis) log.warn(message)
    else if (duration > 200.millis) log.info(message)
    else log.debug(message)
    result
  }

  def first[T, E <: Effect](io: IO[Seq[T], E], onEmpty: => String): IO[T, E] =
    io.flatMap { ts =>
      ts.headOption.map { t =>
        IO.successful(t)
      }.getOrElse { IO.failed(new Exception(onEmpty)) }
    }

  def fail(message: String): Nothing = PimpDatabase.fail(message)
}
