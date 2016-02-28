package tests

import java.nio.file.Path

import com.malliina.musicpimp.db.{DataFolder, DataTrack}
import org.scalatest.FunSuite
import slick.jdbc.meta.MTable
import slick.lifted.AbstractTable


/**
 * @author Michael
 */
class DatabaseTests extends FunSuite {
  val testRootOpt: Option[Path] = None
  //  Library.rootFolders = Seq(testRoot)
  val testFolders = Seq(
    DataFolder.root,
    DataFolder("F1", "Music", "F1", ""),
    DataFolder("F2", "Music2", "F1\\F2", "F1"),
    DataFolder("F3", "Music2", "F1\\F2\\F3", "F2")
  )
  val folder1 = testFolders(1)
  val folder1Id = folder1.id
  val folder2 = testFolders(2)
  val folder2Id = folder2.id
  val testTracks = Seq(
    DataTrack.fromValues("1", "Aces High", "Iron Maiden", "Powerslave", 100, 1000000, folder1Id),
    DataTrack.fromValues("2", "So What", "Pink", "Funhouse", 120, 2000000, folder1Id),
    DataTrack.fromValues("3", "Under the Waves", "Pendulum", "Immersion", 234, 12356889, folder2Id),
    DataTrack.fromValues("4", "Witchcraft", "Pendulum", "Immersion", 100, 1234567, folder2Id),
    DataTrack.fromValues("5", "A Track", "Pendulum", "Immersion", 123, 3455789, folder2Id)
  )
  val tracksInFolder2 = testTracks.slice(2, 5)

  import slick.driver.H2Driver.api._

  case class Query(query: String, other: String)

  case class Query4(one: String, two: String, three: String, four: String)

  test("can insert into database") {
    //    PimpDb.dropAll()
    //    PimpDb.init()
    //    PimpDb.withSession(implicit session => {
    //      PimpDb.folders ++= Library.folderStream
    //      PimpDb.folders += DataFolder.root
    //      Library.folderStream.foreach(f => {
    //        println(f)
    //        PimpDb.folders += f
    //      })
    //      PimpDb.folders ++= Library.folderStream
    //    })
  }

  test("can create database table, query") {
    //    PimpDb.dropAll()
    //    PimpDb.init()
    //    PimpDb.withSession(implicit session => {
    //      PimpDb.folders ++= testFolders
    //      PimpDb.tracks ++= testTracks
    //      val ts = PimpDb.fullText("dgfhfh")
    //      assert(ts.isEmpty)
    //      val ts2 = PimpDb.fullText("Maiden")
    //      assert(ts2.size === 1)
    //      assert(ts2.head === testTracks.head)
    //      val (tracks, folders) = PimpDb.folder(folder2Id)
    //      assert(tracks === tracksInFolder2)
    //      assert(folders.head === testFolders(3))
    //    })
  }

  //  def plainSQL(): Unit = {
  //    implicit val conv = PimpDb.dataResult
  //    PimpDb.database.withSession(implicit session => {
  //      val tracks = (sql"select * from TRACKS").as[DataTrack].list
  //      tracks.foreach(track => println(track))
  //    })
  //  }

//  def createIfNotExists[T <: Table[_]](tables: TableQuery[T]*) = {
//    tables.filter(t => !exists(t)).foreach(t => t.schema.create)
//  }

//  def exists[T <: AbstractTable[_]](table: TableQuery[T]) = {
//    MTable.getTables(table.baseTableRow.tableName).list(session).nonEmpty
//  }
}
