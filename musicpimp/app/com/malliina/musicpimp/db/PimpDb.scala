package com.malliina.musicpimp.db

import java.nio.file.{Files, Path, Paths}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.StorageFile
import com.malliina.musicpimp.db.PimpDb.log
import com.malliina.musicpimp.db.PimpSchema.{folders, tempFoldersTable, tempTracksTable, tracks}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.FolderID
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.Utils
import org.h2.jdbcx.JdbcConnectionPool
import play.api.Logger
import rx.lang.scala.{Observable, Observer, Subject}
import slick.driver.H2Driver.api._
import slick.jdbc.GetResult.GetInt
import slick.jdbc.{GetResult, PositionedResult}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.language.higherKinds
import scala.util.{Failure, Success}

object PimpDb {
  private val log = Logger(getClass)
  val H2UrlSettings = "h2.url.settings"
  val H2Home = "h2.home"

  def default() = {
    val dirByConf: Option[Path] = sys.props.get(H2Home).map(p => Paths.get(p))
    val dataHome: Path = dirByConf getOrElse (FileUtil.pimpHomeDir / "db")
    file(dataHome / "pimp291")
  }

  /**
    * @param path path to database file
    * @return a file-based database stored at `path`
    */
  def file(path: Path) = {
    Option(path.getParent).foreach(p => Files.createDirectories(p))
    new PimpDb(path.toString)
  }

  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  def test() = new PimpDb("mem:test")
}

class PimpDb(conn: String) extends DatabaseLike with AutoCloseable {
  val databaseUrlSettings = sys.props.get(PimpDb.H2UrlSettings)
    .map(_.trim)
    .filter(_.nonEmpty)
    .map(ss => s";$ss")
    .getOrElse("")
  val url = s"jdbc:h2:$conn;DB_CLOSE_DELAY=-1$databaseUrlSettings"
  log info s"Connecting to: $url"
  val pool = JdbcConnectionPool.create(url, "", "")
  override val database = Database.forDataSource(pool)
  val tracksName = PimpSchema.tracks.baseTableRow.tableName
  override val tableQueries = PimpSchema.tableQueries

  def fullText(searchTerm: String, limit: Int = 1000, tableName: String = tracksName): Future[Seq[DataTrack]] = {
    log debug s"Querying: $searchTerm"
    val action = sql"""SELECT T.* FROM FT_SEARCH_DATA($searchTerm,0,0) FT, #$tableName T WHERE FT.TABLE='#$tableName' AND T.ID=FT.KEYS[0] LIMIT $limit;""".as[DataTrack]
    run(action)
  }

  def folder(id: FolderID): Future[(Seq[DataTrack], Seq[DataFolder])] = {
    val tracksQuery = tracks.filter(_.folder === id)
    // '=!=' in slick-lang is the same as '!='
    val foldersQuery = folders.filter(f => f.parent === id && f.id =!= Library.RootId).sortBy(_.title)
    //    println(tracksQuery.selectStatement + "\n" + foldersQuery.selectStatement)
    val tracksFuture = runQuery(tracksQuery)
    val foldersFuture = runQuery(foldersQuery)
    for {
      ts <- tracksFuture
      fs <- foldersFuture
    } yield (ts, fs)
  }

  def folderOnly(id: FolderID): Future[Option[DataFolder]] =
    run(folders.filter(folder => folder.id === id).result.headOption)

  def allTracks = runQuery(tracks)

  def trackCount = run(tracks.length.result)

  def merge(items: Seq[DataTrack]) = {
    val inserts = items map sqlify mkString ","
    val sql = sqlu"MERGE INTO $tracksName KEY(ID) VALUES $inserts"
    executePlain(sql)
  }

  def insertIfNotExists(tracks: Seq[DataTrack]) = merge(tracks)

  def sqlify(track: DataTrack) = s"('${track.id}','${track.artist}','${track.album}','${track.title}')"

  override def initTable[T <: Table[_]](table: TableQuery[T]): Unit = {
    super.initTable(table)
    val name = table.baseTableRow.tableName
    if (name == tracksName) {
      await {
        initIndex(tracksName) recover {
          case t: Throwable =>
            log.warn(s"Initialization of index of table $tracksName failed", t)
            throw t
        }
      }
    }
  }

  object GetDummy extends GetResult[Int] {
    override def apply(v1: PositionedResult) = 0
  }

  /** Fails if the index is already created or if the table does not exist.
    *
    * TODO check when this throws and whether I'm calling it correctly
    */
  def initIndex(tableName: String): Future[Unit] = {
    val clazz = "\"org.h2.fulltext.FullText.init\""
    for {
      _ <- executePlain(sqlu"CREATE ALIAS IF NOT EXISTS FT_INIT FOR #$clazz;")
      _ <- database.run(sql"CALL FT_INIT();".as[Int](GetDummy))
      _ <- database.run(sql"CALL FT_CREATE_INDEX('PUBLIC', '#$tableName', NULL);".as[Int](GetDummy))
    } yield {
      log info s"Initialized index for table $tableName"
    }
  }

  def dropAll() = {
    tableQueries foreach { t =>
      if (exists(t)) {
        await(executePlain(sqlu"DROP TABLE ${t.baseTableRow.tableName};"))
        // ddl.drop fails if the table has a constraint to something nonexistent
        //        t.ddl.drop
        log info s"Dropped table: ${t.baseTableRow.tableName}"
      }
    }
    dropIndex(tracksName)
  }

  def dropIndex(tableName: String): Future[Unit] = {
    val q = sqlu"CALL FT_DROP_INDEX('PUBLIC','#${tableName}');"
    executePlain(q).map(_ => ())
  }

  /** Starts indexing on a background thread and returns an [[Observable]] with progress updates.
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
  def refreshIndex(): Observable[Long] = observe { observer =>
    observer onNext 0L
    // we only want one thread to index at a time
    this.synchronized {
      val (result, duration) = Utils.timed {
        await(runIndexer(observer), 3.hours)
      }
      log info s"Indexing complete in $duration. Indexed ${result.totalFiles} files, " +
        s"purged ${result.foldersPurged} folders and ${result.tracksPurged} files."
    }
  }

  private def runIndexer(observer: Observer[Long]): Future[IndexResult] = {
    log info "Indexing..."
    // deletes any old rows from previous indexings
    val firstIdsDeletion = tempFoldersTable.delete
    val musicFolders = Library.folderStream
    // upserts every folder in the library to the database
    val updateActions = musicFolders.map(folder => folders.insertOrUpdate(folder))
    val folderUpdates = DBIO.sequence(updateActions)
    // adds every existing folder to the temp table
    val idInsertion = tempFoldersTable ++= musicFolders.map(f => TempFolder(f.id))
    // deletes non-existing folders
    val foldersDeletion = folders.filterNot(f => f.id.in(tempFoldersTable.map(i => i.id))).delete
    val secondIdsDeletion = tempFoldersTable.delete
    val foldersInit = DBIO.seq(
      firstIdsDeletion,
      folderUpdates,
      idInsertion)

    def updateFolders() = for {
      _ <- run(foldersInit)
      foldersDeleted <- run(foldersDeletion)
      _ <- run(secondIdsDeletion)
    } yield foldersDeleted

    // repeat above, but for tracks
    val oldTrackDeletion = tempTracksTable.delete
    var fileCount = 0L

    def upsertAllTracks() = {
      Library.dataTrackStream.grouped(100) foreach { chunk =>
        val trackUpdates = DBIO.sequence(chunk.map(track => tracks.insertOrUpdate(track)))
        val chunkInsertion = run(DBIO.seq(
          trackUpdates,
          tempTracksTable ++= chunk.map(t => TempTrack(t.id))
        ))
        await(chunkInsertion, 1.hour)
        fileCount += chunk.size
        observer onNext fileCount
      }
    }

    val tracksDeletion = tracks.filterNot(t => t.id.in(tempTracksTable.map(_.id))).delete
    val thirdIdsDeletion = tempFoldersTable.delete

    def deleteNonExistentTracks() = for {
      tracksDeleted <- run(tracksDeletion)
      _ <- run(thirdIdsDeletion)
    } yield tracksDeleted

    def updateTracks() = for {
      _ <- run(oldTrackDeletion)
      _ = upsertAllTracks()
      ts <- deleteNonExistentTracks()
    } yield ts

    for {
      fs <- updateFolders()
      ts <- updateTracks()
    } yield IndexResult(fileCount, fs, ts)
  }

  def observe[T](f: Observer[T] => Unit): Observable[T] = {
    val subject = Subject[T]()
    Future(f(subject)) onComplete {
      case Success(_) => subject.onCompleted()
      case Failure(t) => subject.onError(t)
    }
    subject
  }

  def runQuery[A, B, C[_]](query: Query[A, B, C]): Future[C[B]] = run(query.result)

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = database.run(a)

  def await[T](f: Future[T], dur: FiniteDuration = 5.seconds): T =
    Await.result(f, dur)

  def close(): Unit = {
    database.close()
    pool.dispose()
  }
}

case class IndexResult(totalFiles: Long, foldersPurged: Int, tracksPurged: Int)
