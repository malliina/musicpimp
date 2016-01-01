package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.db.DatabaseLike.log
import play.api.Logger

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.{GetResult, SetParameter, StaticQuery}
import scala.slick.lifted.AbstractTable
import scala.util.Try

/**
 * @author Michael
 */
trait DatabaseLike {
  def database: Database

  def tableQueries: Seq[TableQuery[_ <: Table[_]]]

  def init(): Unit = {
    withSession(implicit session => {
      log info s"Ensuring all tables exist..."
      createIfNotExists(tableQueries: _*)
    })
  }

  def withSession[T](f: Session => T): Future[T] = Future.fromTry(Try(database withSession f))

  def exists[T <: AbstractTable[_]](table: TableQuery[T])(implicit session: Session) = {
    val tableName = table.baseTableRow.tableName
    try {
      MTable.getTables(tableName).list(session).nonEmpty
    } catch {
      case sqle: SQLException =>
        log.error(s"Unable to verify table: $tableName", sqle)
        false
    }
  }

  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*)(implicit session: Session) =
    tables.reverse.filter(t => !exists(t)(session)).foreach(t => initTable(t))

  def initTable[T <: Table[_]](table: TableQuery[T])(implicit session: Session) = {
    table.ddl.create(session)
    val msg = s"Created table: ${table.baseTableRow.tableName}"
    log info msg
  }

  def executePlain(queries: String*) =
    withSession(s => queries.foreach(q => StaticQuery.updateNA(q).execute(s)))

  def queryPlain[R](query: String)(implicit rconv: GetResult[R]) =
    withSession(s => StaticQuery.queryNA[R](query).list(s))

  def queryPlainParam[R, P](query: String, param: P)(implicit pconv: SetParameter[P], rconv: GetResult[R]) = {
    val q = StaticQuery.query[P, R](query)
    withSession(s => q(param).list(s))
  }
}

object DatabaseLike {
  private val log = Logger(getClass)
}
