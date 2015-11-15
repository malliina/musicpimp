package com.mle.musicpimp.db

import java.nio.file.{Files, Path, Paths}

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.file.StorageFile
import com.mle.musicpimp.db.PimpSchema.{folders, idsTable, tracks}
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.util.FileUtil
import com.mle.storage.StorageLong
import com.mle.util.{Log, Utils}
import org.h2.jdbcx.JdbcConnectionPool
import rx.lang.scala.{Observable, Observer, Subject}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.GetResult
import scala.util.{Failure, Success, Try}

/**
  * @author Michael
  */
class PimpDb extends DatabaseLike with Log with AutoCloseable {
  val H2_URL_SETTINGS = "h2.url.settings"
  val H2_HOME = "h2.home"
  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  //  override val database = Database.forURL("jdbc:h2:~/.musicpimp/pimp;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  val databaseUrlSettings = sys.props.get(H2_URL_SETTINGS).map(_.trim).filter(_.nonEmpty).map(ss => s";$ss") getOrElse ""
  val dirByConf: Option[Path] = sys.props.get(H2_HOME).map(p => Paths.get(p))
  val dataHome: Path = dirByConf getOrElse (FileUtil.pimpHomeDir / "db")
  Files.createDirectories(dataHome)
  val databaseFile = dataHome / "pimp291"
  val url = s"jdbc:h2:$databaseFile;DB_CLOSE_DELAY=-1$databaseUrlSettings"
  log info s"Connecting to: $url"
  val pool = JdbcConnectionPool.create(url, "", "")
  override val database = Database.forDataSource(pool)
  val tracksName = PimpSchema.tracks.baseTableRow.tableName
  override val tableQueries = PimpSchema.tableQueries
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<, r.nextInt().seconds, r.nextLong().bytes, r.<<))

  def fullText(searchTerm: String, limit: Int = 1000, tableName: String = tracksName): Future[Seq[DataTrack]] = {
    log debug s"Querying: $searchTerm"
    queryPlainParam[DataTrack, String](s"SELECT T.* FROM FT_SEARCH_DATA(?,0,0) FT, $tableName T WHERE FT.TABLE='$tableName' AND T.ID=FT.KEYS[0] LIMIT $limit;", searchTerm)
  }

  def folder(id: String): Future[(Seq[DataTrack], Seq[DataFolder])] = withSession(implicit s => {
    val tracksQuery = tracks.filter(_.folder === id)
    // '=!=' in slick-lang is the same as '!='
    val foldersQuery = folders.filter(f => f.parent === id && f.id =!= Library.ROOT_ID).sortBy(_.title)
    //    println(tracksQuery.selectStatement + "\n" + foldersQuery.selectStatement)
    (tracksQuery.run, foldersQuery.run)
  })

  def folderOnly(id: String): Future[Option[DataFolder]] = withSession(implicit s => {
    folders.filter(folder => folder.id === id).firstOption
  })

  def allTracks = withSession(tracks.list(_))

  def trackCount = withSession(tracks.size.run(_))

  def merge(items: Seq[DataTrack]) = withSession(implicit s => {
    val inserts = items map sqlify mkString ","
    val sql = s"MERGE INTO $tracksName KEY(ID) VALUES $inserts"
    executePlain(sql)
  })

  def insertIfNotExists(tracks: Seq[DataTrack]) = merge(tracks)

  def sqlify(track: DataTrack) = s"('${track.id}','${track.artist}','${track.album}','${track.title}')"

  override def initTable[T <: Table[_]](table: TableQuery[T])(implicit session: Session): Unit = {
    super.initTable(table)
    val name = table.baseTableRow.tableName
    if (name == tracksName) {
      initIndex(tracksName).recover {
        case t: Throwable => log.warn(s"Indexing failed: $t: ${t.getMessage}", t)
      }
    }
  }

  /**
    * Fails if the index is already created or if the table does not exist.
    *
    * TODO check when this throws and whether I'm calling it correctly
    * @param session
    */
  def initIndex(tableName: String)(implicit session: Session): Try[Unit] = Try {
    executePlain(
      "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";",
      "CALL FT_INIT();",
      s"CALL FT_CREATE_INDEX('PUBLIC', '$tableName', NULL);"
    )
    log info s"Initialized index for table $tableName"
  }

  def dropAll() = withSession(implicit s => {
    tableQueries.foreach(t => {
      if (exists(t)) {
        executePlain(s"DROP TABLE ${t.baseTableRow.tableName}")
        // ddl.drop fails if the table has a constraint to something nonexistent
        //        t.ddl.drop
      }
      log info s"Dropped table: ${t.baseTableRow.tableName}"
    })
    dropIndex(tracksName)
  })

  def dropIndex(tableName: String)(implicit s: Session): Try[Unit] = Try {
    executePlain(s"CALL FT_DROP_INDEX('PUBLIC','$tableName');")
  }

  /**
    * Starts indexing on a background thread and returns an [[Observable]] with progress updates.
    *
    * This algorithm adds new tracks and folders to the index, and removes tracks and folders that no longer exist in
    * the library.
    *
    * Implementation:
    *
    * 1) Upsert all folders to the folders table, based on the (authoritative) library
    * 2) Put all existing folder IDs also into a temporary table (also based on the library)
    * 3) Delete all folders which don't have IDs in the temporary table
    * 4) Delete the temporary table
    * 5) Do the same thing for tracks (go to step 1)
    *
    * @return progress: total amount of files indexed
    */
  def refreshIndex(): Observable[Long] = observe(observer => {
    observer onNext 0L
    // we only want one thread to index at a time
    this.synchronized {
      val ((fileCount, foldersPurged, tracksPurged), duration) = Utils.timed {
        log info "Indexing..."
        val f = withSession(implicit session => {
          idsTable.delete
          val musicFolders = Library.folderStream
          musicFolders.foreach(folder => folders.insertOrUpdate(folder))
          idsTable.insertAll(musicFolders.map(f => Id(f.id)): _*)
          val foldersDeleted = folders.filterNot(f => f.id.in(idsTable.map(_.id))).delete
          idsTable.delete
          var fileCount = 0L
          Library.dataTrackStream.grouped(100).foreach(chunk => {
            chunk.foreach(track => tracks.insertOrUpdate(track))
            idsTable.insertAll(chunk.map(t => Id(t.id)): _*)
            fileCount += chunk.size
            observer onNext fileCount
          })
          val tracksDeleted = tracks.filterNot(t => t.id.in(idsTable.map(_.id))).delete
          idsTable.delete
          (fileCount, foldersDeleted, tracksDeleted)
        })
        // TODO
        Await.result(f, 3.hours)
      }
      log info s"Indexing complete in $duration. Indexed $fileCount files, purged $foldersPurged folders and $tracksPurged files."
    }
  })

  def observe[T](f: Observer[T] => Unit): Observable[T] = {
    val subject = Subject[T]()
    Future(f(subject)).onComplete {
      case Success(u) => subject.onCompleted()
      case Failure(t) => subject.onError(t)
    }
    subject
  }

  def close(): Unit = {
    pool.dispose()
  }
}

case class IndexResult(totalFiles: Int, foldersPurged: Int, tracksPurged: Int)
