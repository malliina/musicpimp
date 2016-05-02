package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.db.DatabaseLike.log
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable
import slick.lifted.{AbstractTable, TableQuery}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

trait DatabaseLike {
  def database: Database

  def tableQueries: Seq[TableQuery[_ <: Table[_]]]

  def init(): Unit = {
    log info s"Ensuring all tables exist..."
    createIfNotExists(tableQueries: _*)
  }

  //  def withSession[T](f: Session => T): Future[T] = Future.fromTry(Try(database withSession f))

  def exists[T <: AbstractTable[_]](table: TableQuery[T]): Boolean = {
    val tableName = table.baseTableRow.tableName
    try {
      val future = database.run(MTable.getTables(tableName))
      await(future).nonEmpty
    } catch {
      case sqle: SQLException =>
        log.error(s"Unable to verify table: $tableName", sqle)
        false
    }
  }


  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*): Unit =
    tables.reverse.filter(t => !exists(t)).foreach(t => initTable(t))

  def initTable[T <: Table[_]](table: TableQuery[T]) = {
    await(database.run(table.schema.create))
    log info s"Created table: ${table.baseTableRow.tableName}"
  }

  def executePlain(queries: String*): Future[Seq[Int]] =
    Future.traverse(queries)(query => database.run(sqlu"""$query"""))

  //  def queryPlain[R](query: String)(implicit rconv: GetResult[R]): Future[Seq[R]] = {
  //    val action = sql"""$query""".as[R]
  //    database.run(action)
  //  }

  private def await[T](f: Future[T]): T = Await.result(f, 10.seconds)
}

object DatabaseLike {
  private val log = Logger(getClass)
}
