package com.malliina.musicpimp.db

import akka.actor.ActorSystem
import com.zaxxer.hikari.HikariDataSource
import io.getquill._
import io.getquill.context.jdbc.MysqlJdbcContextBase
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

class PimpMySQL(ds: HikariDataSource, ec: ExecutionContext)
  extends PimpMySQLDatabase(ds, CompositeNamingStrategy2(SnakeCase, UpperCase), ec)

object PimpMySQLDatabase {
  def withMigrations(as: ActorSystem, conf: Conf): PimpMySQL = {
    val flyway =
      Flyway.configure
        .dataSource(conf.url, conf.user, conf.pass)
        .baselineVersion("1")
        .baselineOnMigrate(true)
        .load()
    flyway.migrate()
    apply(as, conf)
  }

  private def apply(as: ActorSystem, dbConf: Conf): PimpMySQL = {
    val pool = as.dispatchers.lookup("contexts.database")
    apply(Conf.dataSource(dbConf), pool)
  }

  private def apply(ds: HikariDataSource, ec: ExecutionContext): PimpMySQL = {
    new PimpMySQL(ds, ec)
  }

  def fail(message: String): Nothing = throw new Exception(message)
}

class PimpMySQLDatabase[N <: NamingStrategy](
  val dataSource: HikariDataSource,
  naming: N,
  ec: ExecutionContext
) extends PortableDatabase[MySQLDialect, N](naming, ec)
  with MysqlJdbcContextBase[N]
