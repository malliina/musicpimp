package com.mle.musicpimp.db

import java.nio.file.{Paths, Path, Files}

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.file.FileUtilities.{userHome, userDir, tempDir}
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.util.FileUtil
import com.mle.storage.StorageLong
import com.mle.util.Log
import org.h2.jdbcx.JdbcConnectionPool
import rx.lang.scala.{Observable, Observer, Subject}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.GetResult
import scala.util.{Failure, Success, Try}
import com.mle.file.StorageFile
/**
 * @author Michael
 */
object PimpDb extends PimpDatabase with Log {
  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  //  override val database = Database.forURL("jdbc:h2:~/.musicpimp/pimp;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  val databaseUrlSettings = sys.props.get("h2.url.settings").map(_.trim).filter(_.nonEmpty).map(ss => s";$ss") getOrElse ""
  // prefers h2.home, otherwise a subdir under user.dir, user.home or finally the temp dir, whichever works first
  val dirByConf: Option[Path] = sys.props.get("h2.home").map(p => Paths.get(p))
  def secondaryDir = Seq(userDir, userHome).find(Files.isWritable).map(dir => dir / ".musicpimp" / "db")
  val dataHome: Path = (dirByConf orElse secondaryDir).getOrElse(tempDir)
  Files.createDirectories(dataHome)
  val url = s"jdbc:h2:$dataHome/pimp265;DB_CLOSE_DELAY=-1$databaseUrlSettings"
  log info s"Connecting to: $url"
  val pool = JdbcConnectionPool.create(url, "", "")
  override val database = Database.forDataSource(pool)

  val tracksName = tracks.baseTableRow.tableName
  override val tableQueries = Seq(tracks, folders, tokens, usersTable)
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<, r.nextInt().seconds, r.nextLong().bytes, r.<<))

  def fullText(searchTerm: String, limit: Int = 1000, tableName: String = tracksName): Seq[DataTrack] = {
    log debug s"Querying: $searchTerm"
    queryPlainParam[DataTrack, String](s"SELECT T.* FROM FT_SEARCH_DATA(?,0,0) FT, $tableName T WHERE FT.TABLE='$tableName' AND T.ID=FT.KEYS[0] LIMIT $limit;", searchTerm)
  }

  def folder(id: String): (Seq[DataTrack], Seq[DataFolder]) = withSession(implicit s => {
    val tracksQuery = tracks.filter(_.folder === id)
    // '=!=' in slick-lang is the same as '!='
    val foldersQuery = folders.filter(f => f.parent === id && f.id =!= Library.ROOT_ID).sortBy(_.title)
    //    println(tracksQuery.selectStatement + "\n" + foldersQuery.selectStatement)
    (tracksQuery.run, foldersQuery.run)
  })

  def allTracks = withSession(tracks.list(_))

  def trackCount = withSession(tracks.size.run(_))

  def merge(items: Seq[DataTrack]) = withSession(implicit session => {
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
    PimpDb.executePlain(
      "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";",
      "CALL FT_INIT();",
      s"CALL FT_CREATE_INDEX('PUBLIC', '$tableName', NULL);"
    )
    log info s"Initialized index for table $tableName"
  }

  def dropAll() = withSession(implicit s => {
    tableQueries.foreach(t => {
      if (exists(t)) {
        PimpDb.executePlain(s"DROP TABLE ${t.baseTableRow.tableName}")
        // ddl.drop fails if the table has a constraint to something nonexistent
        //        t.ddl.drop
      }
      log info s"Dropped table: ${t.baseTableRow.tableName}"
    })
    dropIndex(tracksName)
  })

  def dropIndex(tableName: String)(implicit s: Session): Try[Unit] = Try {
    PimpDb.executePlain(s"CALL FT_DROP_INDEX('PUBLIC','$tableName');")
  }

  /**
   * Starts indexing on a background thread and returns an [[Observable]] with progress updates.
   *
   * @return progress: total amount of files indexed
   */
  def refreshIndex(): Observable[Long] = observe(observer => {
    observer onNext 0L
    // we only want one thread to index at a time
    this.synchronized {
      log info "Indexing..."
      withSession(implicit session => {
        tracks.delete
        folders.delete
        folders ++= Library.folderStream
        var fileCount = 0L
        Library.dataTrackStream.grouped(100).foreach(chunk => {
          tracks ++= chunk
          fileCount += chunk.size
          observer onNext fileCount
        })
        log info s"Indexed $fileCount files."
      })
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
}
