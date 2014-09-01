package tests

import com.mle.musicpimp.db.{DataTrack, PimpDb}
import org.scalatest.FunSuite

import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.AbstractTable
import scala.util.Try

/**
 * @author Michael
 */
class DatabaseTests extends FunSuite {
  val testData = Seq(
    DataTrack.fromValues("1", "Aces High", "Iron Maiden", "Powerslave", 100, 1000000),
    DataTrack.fromValues("2", "So What", "Pink", "Funhouse", 120, 2000000),
    DataTrack.fromValues("3", "Under the Waves", "Pendulum", "Immersion", 234, 12356889),
    DataTrack.fromValues("4", "Witchcraft", "Pendulum", "Immersion", 0, 0)
  )

  import scala.slick.driver.H2Driver.simple._

  case class Query(query: String, other: String)

  case class Query4(one: String, two: String, three: String, four: String)

  test("can create database table, query") {
    Try(PimpDb.withSession(implicit s => PimpDb.dropTracks()))
    PimpDb.init()
    PimpDb.withSession(implicit session => {
      PimpDb.tracks ++= testData
      PimpDb.tracks.list.foreach(println)
      val ts = PimpDb.fullText("dgfhfh")
      assert(ts.isEmpty)
      val ts2 = PimpDb.fullText("Maiden")
      assert(ts2.size === 1)
      assert(ts2.head === testData.head)
    })
  }

  def plainSQL(): Unit = {
    implicit val conv = PimpDb.dataResult
    PimpDb.database.withSession(implicit session => {
      val tracks = sql"select * from TRACKS".as[DataTrack].list
      tracks.foreach(track => println(track))
    })
  }

  def exists[T <: AbstractTable[_]](table: TableQuery[T])(implicit session: Session) = {
    MTable.getTables(table.baseTableRow.tableName).list(session).nonEmpty
  }

  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*)(implicit session: Session) = {
    tables.filter(t => !exists(t)(session)).foreach(t => t.ddl.create)
  }
}
