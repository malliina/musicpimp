package com.malliina.musicpimp.db

import java.sql.SQLException

import com.malliina.musicpimp.db.DatabaseLike.log
import play.api.Logger
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slick.lifted.AbstractTable

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class DatabaseLike(val profile: JdbcProfile) {

  import profile.api._

  def ec: ExecutionContext

  def database: JdbcProfile#Backend#Database

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
