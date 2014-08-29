package com.mle.musicpimp.db

import com.mle.db.DatabaseLike
import com.mle.musicpimp.library.Library
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.util.Log
import rx.lang.scala.{Observable, Observer, Subject, Subscriber}

import scala.concurrent.Future
import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.GetResult
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
object PimpDb extends DatabaseLike with Log {
  val testData = Seq(
    DataTrack("1", "Iron Maiden", "Powerslave", "Aces High"),
    DataTrack("2", "Pink", "Funhouse", "So What"),
    DataTrack("3", "Pendulum", "Immersion", "Under the Waves"),
    DataTrack("4", "Pendulum", "Immersion", "Witchcraft")
  )

  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  override val database = Database.forURL("jdbc:h2:~/.musicpimp/pimp;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  val tracks = TableQuery[Tracks]
  override val tableQueries = Seq(tracks)
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<))

  def fullText(query: String, tableName: String = tracks.baseTableRow.tableName): Seq[DataTrack] = {
    log debug s"Querying: $query"
    queryPlainParam[DataTrack, String](s"SELECT T.* FROM FT_SEARCH_DATA(?,0,0) FT, $tableName T WHERE FT.TABLE='$tableName' AND T.ID=FT.KEYS[0];", query)
  }

  override def onInit(implicit session: H2Driver.simple.Session): Unit = {
    initIndex(session)
  }

  def allTracks = withSession(tracks.list(_))

  def trackCount = withSession(s => tracks.size.run(s))

  def merge(items: Seq[DataTrack]) = withSession(implicit session => {
    val inserts = items map sqlify mkString ","
    val sql = s"MERGE INTO TRACKS KEY(ID) VALUES $inserts"
    executePlain(sql)
  })

  def insertIfNotExists(tracks: Seq[DataTrack]) = merge(tracks)

  def sqlify(track: DataTrack) = s"('${track.id}','${track.artist}','${track.album}','${track.track}')"

  /**
   * Fails if the index is already created or if the table does not exist.
   *
   * TODO check when this throws and whether I'm calling it correctly
   * @param session
   */
  def initIndex(implicit session: Session): Try[Unit] = Try {
    log info s"Initializing index..."
    PimpDb.executePlain(
      "CREATE ALIAS IF NOT EXISTS FT_INIT FOR \"org.h2.fulltext.FullText.init\";",
      "CALL FT_INIT();",
      "CALL FT_CREATE_INDEX('PUBLIC', 'TRACKS', NULL);"
    )
  }

  /**
   * Starts indexing on a background thread and returns an [[Observable]] with progress updates.
   *
   * @return progress: total amount of files indexed
   */
  def refreshIndex(): Observable[Long] = {
    observe[Long](obs => {
      this.synchronized {
        log info "Indexing..."
        withSession(implicit session => {
          tracks.delete
          var fileCount = 0L
          Library.dataTrackStream.grouped(999).foreach(stream => {
            tracks ++= stream
            fileCount += stream.size
            obs onNext fileCount
          })
          log info s"Files indexed: $fileCount"
        })
      }
    })
  }

  def tryObserve[T, U](f: Subscriber[T] => U) = Observable[T](sub => {
    Try(f(sub)) match {
      case Success(u) => sub.onCompleted()
      case Failure(t) => sub onError t
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
