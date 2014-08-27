package tests

import com.mle.musicpimp.db.{DataTrack, PimpDb}
import controllers.Search
import org.scalatest.FunSuite

import scala.slick.jdbc.GetResult
import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.AbstractTable

/**
 * @author Michael
 */
class DatabaseTests extends FunSuite {

  import scala.slick.driver.H2Driver.simple._

  case class Query(query: String, other: String)

  case class Query4(one: String, two: String, three: String, four: String)

  test("can create database table, query") {
    init()
    PimpDb.database.withSession(implicit session => {
      initIndex(session)
      val ts = PimpDb.fullText("Immersion")
      println(ts)
      val ts2 = PimpDb.fullText("Maiden")
      println(ts2)
    })
  }

  def plainSQL(): Unit = {
    implicit val getTrackResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<))
    PimpDb.database.withSession(implicit session => {
      val tracks = sql"select * from TRACKS".as[DataTrack].list
      tracks.foreach(track => println(track))
    })
  }

  def init(): Unit = {
    PimpDb.database.withSession(implicit session => {
      createIfNotExists(PimpDb.tracks)
      PimpDb.tracks ++= Search.testData
    })
  }

  def initIndex(implicit session: Session): Unit = {
    PimpDb.executePlain(
      "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";",
      "CALL FT_INIT();",
      "CALL FT_CREATE_INDEX('PUBLIC', 'TRACKS', NULL);"
    )
  }

  def exists[T <: AbstractTable[_]](table: TableQuery[T])(implicit session: Session) = {
    MTable.getTables(table.baseTableRow.tableName).list(session).nonEmpty
  }

  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*)(implicit session: Session) = {
    tables.filter(t => !exists(t)(session)).foreach(t => t.ddl.create)
  }
}
