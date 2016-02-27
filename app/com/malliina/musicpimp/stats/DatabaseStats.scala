package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.{PimpDb, PimpSchema, PlaybackRecord, Sessionizer}
import com.malliina.musicpimp.models.User
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._

class DatabaseStats(db: PimpDb) extends Sessionizer(db) with PlaybackStats {
  val plays = PimpSchema.plays
  val tracks = PimpSchema.tracks
  val users = PimpSchema.usersTable

  override def played(track: TrackMeta, user: User): Future[Unit] = withSession { implicit s =>
    plays.insert(PlaybackRecord(track.id, DateTime.now, user))
  }

  override def mostRecent(user: User, count: Int): Future[Seq[RecentEntry]] = {
    val sortedHistory = playbackHistoryQuery(user)
      .sortBy(_._1.when.desc)
      .take(count)
    withSession(s => sortedHistory.run(s).map {
      case (record, track) => RecentEntry(track, record.when)
    })
  }

  override def mostPlayed(user: User): Future[Seq[MostPlayedEntry]] = {
    val query = playbackHistoryQuery(user).groupBy {
      case (record, track) => track
    }.map {
      case (track, rs) => (track, rs.length)
    }

    withSession(s => query.run(s)).map(_.map {
      case (track, count) => PimpMostPlayedEntry(track, count)
    })
  }

  private def playbackHistoryQuery(user: User) = for {
    record <- plays if record.who === user
    track <- tracks if track.id === record.track
  } yield (record, track)
}
