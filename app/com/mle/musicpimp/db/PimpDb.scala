package com.mle.musicpimp.db

import com.mle.db.DatabaseLike
import com.mle.musicpimp.library.Library
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import rx.lang.scala.{Observable, Observer, Subject, Subscriber}

import scala.concurrent.Future
import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.GetResult
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
object PimpDb extends DatabaseLike {
  val testData = Seq(
    DataTrack("1", "Iron Maiden", "Powerslave", "Aces High"),
    DataTrack("2", "Pink", "Funhouse", "So What"),
    DataTrack("3", "Pendulum", "Immersion", "Under the Waves"),
    DataTrack("4", "Pendulum", "Immersion", "Witchcraft")
  )

  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  override val database = Database.forURL("jdbc:h2:~/pimp;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  val tracks = TableQuery[Tracks]
  override val tableQueries = Seq(tracks)
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<))

  def fullText(query: String): Seq[DataTrack] = {
    log info s"Querying: $query"
    queryPlainParam[DataTrack, String]("SELECT T.* FROM FT_SEARCH_DATA(?,0,0) FT, TRACKS T WHERE FT.TABLE='TRACKS' AND T.ID=FT.KEYS[0];", query)
  }

  override def onInit(implicit session: H2Driver.simple.Session): Unit = {
    initIndex(session)
    log info s"Inserting test data..."
    merge(testData)
  }

  def allTracks = withSession(tracks.list(_))

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

  def refreshIndex(): Observable[Long] = {
    observe[Long](obs => {
      withSession(implicit session => {
        tracks.delete
        var size = 0L
        Library.dataTrackStream.grouped(999).foreach(stream => {
          tracks ++= stream
          size += stream.size
          obs onNext size
        })
      })
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
