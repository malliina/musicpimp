package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.db.DatabaseLike.log
import play.api.Logger
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable
import slick.lifted.{AbstractTable, TableQuery}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

trait DatabaseLike {
  def ec: ExecutionContext

  def database: Database

  def tableQueries: Seq[TableQuery[_ <: Table[_]]]

  def init(): Unit = {
    log info s"Ensuring all tables exist..."
    createIfNotExists(tableQueries: _*)
  }

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

  def executePlain(queries: DBIOAction[Int, NoStream, Nothing]*): Future[Seq[Int]] =
    sequentially(queries.toList)

  def sequentially(queries: List[DBIOAction[Int, NoStream, Nothing]]): Future[List[Int]] =
    queries match {
      case head :: tail =>
        database.run(head).flatMap(i => sequentially(tail).map(is => i :: is)(ec))(ec)
      case Nil =>
        Future.successful(Nil)
    }

  private def await[T](f: Future[T]): T = Await.result(f, 10.seconds)
}

object DatabaseLike {
  private val log = Logger(getClass)
}
