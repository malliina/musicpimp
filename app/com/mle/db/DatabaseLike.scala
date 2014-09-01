package com.mle.db

import com.mle.util.Log

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.{GetResult, SetParameter, StaticQuery}
import scala.slick.lifted.AbstractTable

/**
 * @author Michael
 */
trait DatabaseLike extends Log {
  def database: Database

  def tableQueries: Seq[TableQuery[_ <: Table[_]]]

  def init(): Unit = {
    withSession(implicit session => {
      log info s"Ensuring all tables exist..."
      createIfNotExists(tableQueries: _*)
    })
    withSession(implicit session => onInit(session))
  }

  def onInit(implicit session: Session): Unit = ()

  def withSession[T](f: Session => T) = database withSession f

  def exists[T <: AbstractTable[_]](table: TableQuery[T])(implicit session: Session) =
    MTable.getTables(table.baseTableRow.tableName).list(session).nonEmpty

  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*)(implicit session: Session) =
    tables.filter(t => !exists(t)(session)).foreach(t => initTable(t))

  def initTable[T <: Table[_]](table: TableQuery[T])(implicit session: Session) = {
    table.ddl.create
    log info s"Created table: ${table.baseTableRow.tableName}"
  }

  def executePlain(queries: String*) =
    withSession(implicit session => queries.foreach(q => StaticQuery.updateNA(q).execute))

  def queryPlain[R](query: String)(implicit rconv: GetResult[R]) =
    withSession(implicit s => StaticQuery.queryNA[R](query).list)

  def queryPlainParam[R, P](query: String, param: P)(implicit pconv: SetParameter[P], rconv: GetResult[R]) = {
    val q = StaticQuery.query[P, R](query)
    withSession(implicit s => q(param).list)
  }
}
