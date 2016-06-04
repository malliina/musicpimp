package com.malliina.musicpimp.db

import java.nio.file.{Files, Path, Paths}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.StorageFile
import com.malliina.musicpimp.db.PimpSchema.{folders, idsTable, tracks}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.{Log, Utils}
import org.h2.jdbcx.JdbcConnectionPool
import rx.lang.scala.{Observable, Observer, Subject}
import slick.driver.H2Driver.api._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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

  def fullText(searchTerm: String, limit: Int = 1000, tableName: String = tracksName): Future[Seq[DataTrack]] = {
    log debug s"Querying: $searchTerm"
    val action = sql"""SELECT T.* FROM FT_SEARCH_DATA($searchTerm,0,0) FT, #$tableName T WHERE FT.TABLE='#$tableName' AND T.ID=FT.KEYS[0] LIMIT $limit;""".as[DataTrack]
    run(action)
  }

  def folder(id: String): Future[(Seq[DataTrack], Seq[DataFolder])] = {
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

  def folderOnly(id: String): Future[Option[DataFolder]] =
    run(folders.filter(folder => folder.id === id).result.headOption)

  def allTracks = runQuery(tracks)

  def trackCount = run(tracks.length.result)

  def merge(items: Seq[DataTrack]) = {
    val inserts = items map sqlify mkString ","
    val sql = s"MERGE INTO $tracksName KEY(ID) VALUES $inserts"
    executePlain(sql)
  }

  def insertIfNotExists(tracks: Seq[DataTrack]) = merge(tracks)

  def sqlify(track: DataTrack) = s"('${track.id}','${track.artist}','${track.album}','${track.title}')"

  override def initTable[T <: Table[_]](table: TableQuery[T]): Unit = {
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
    */
  def initIndex(tableName: String): Future[Unit] =
    executePlain(
      "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";",
      "CALL FT_INIT();",
      s"CALL FT_CREATE_INDEX('PUBLIC', '$tableName', NULL);"
    ).map { _ =>
      log info s"Initialized index for table $tableName"
      ()
    }

  def dropAll() = {
    tableQueries.foreach(t => {
      if (exists(t)) {

        Await.result(executePlain(s"DROP TABLE ${t.baseTableRow.tableName}"), 5.seconds)
        // ddl.drop fails if the table has a constraint to something nonexistent
        //        t.ddl.drop
        log info s"Dropped table: ${t.baseTableRow.tableName}"
      }
    })
    dropIndex(tracksName)
  }

  def dropIndex(tableName: String): Future[Unit] =
    executePlain(s"CALL FT_DROP_INDEX('PUBLIC','$tableName');").map(_ => ())

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
  def refreshIndex(): Observable[Long] = observe(observer => {
    observer onNext 0L
    // we only want one thread to index at a time
    this.synchronized {
      val ((fileCount, foldersPurged, tracksPurged), duration) = Utils.timed {
        log info "Indexing..."
        val firstIdsDeletion = idsTable.delete
        val musicFolders = Library.folderStream
        val updateActions = musicFolders.map(folder => folders.insertOrUpdate(folder))
        val folderUpdates = DBIO.sequence(updateActions)
        val idInsertion = idsTable ++= musicFolders.map(f => Id(f.id))
        val foldersDeletion = folders.filterNot(f => f.id.in(idsTable.map(_.id))).delete
        val secondIdsDeletion = idsTable.delete
        val foldersInit = DBIO.seq(
          firstIdsDeletion,
          folderUpdates,
          idInsertion)
        def updateFolders() = for {
          _ <- run(foldersInit)
          foldersDeleted <- run(foldersDeletion)
          _ <- run(secondIdsDeletion)
        } yield foldersDeleted
        var fileCount = 0L
        def upsertAllTracks() = {
          Library.dataTrackStream.grouped(100).foreach(chunk => {
            val trackUpdates = DBIO.sequence(chunk.map(track => tracks.insertOrUpdate(track)))
            val chunkInsertion = run(DBIO.seq(
              trackUpdates,
              idsTable ++= chunk.map(t => Id(t.id))
            ))
            Await.result(chunkInsertion, 1.hour)
            fileCount += chunk.size
            observer onNext fileCount
          })
        }
        val tracksDeletion = tracks.filterNot(t => t.id.in(idsTable.map(_.id))).delete
        val thirdIdsDeletion = idsTable.delete
        def deleteNonExistentTracks() = for {
          tracksDeleted <- database.run(tracksDeletion)
          _ <- database.run(thirdIdsDeletion)
        } yield tracksDeleted
        val f = for {
          fs <- updateFolders()
          _ = upsertAllTracks()
          ts <- deleteNonExistentTracks()
        } yield (fileCount, fs, ts)
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

  def runQuery[A, B, C[_]](query: Query[A, B, C]): Future[C[B]] = run(query.result)

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = database.run(a)

  def await[T](f: Future[T]): T = Await.result(f, 5.seconds)

  def close(): Unit = {
    database.close()
    pool.dispose()
  }
}

case class IndexResult(totalFiles: Int, foldersPurged: Int, tracksPurged: Int)
