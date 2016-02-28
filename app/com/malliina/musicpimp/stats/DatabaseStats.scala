package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.Mappings.jodaDate
import com.malliina.musicpimp.db.{PimpDb, PimpSchema, PlaybackRecord, Sessionizer}
import com.malliina.musicpimp.models.User
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class DatabaseStats(db: PimpDb) extends Sessionizer(db) with PlaybackStats {
  val plays = PimpSchema.plays
  val tracks = PimpSchema.tracks
  val users = PimpSchema.usersTable

  override def played(track: TrackMeta, user: User): Future[Unit] =
    run(plays += PlaybackRecord(track.id, DateTime.now, user)).map(_ => ())

  override def mostRecent(user: User, count: Int): Future[Seq[RecentEntry]] = {
    val sortedHistory = playbackHistory(user)
      .sortBy(_._1.when.desc)
      .take(count)
    runQuery(sortedHistory).map(_.map {
      case (record, track) => RecentEntry(track, record.when)
    })
  }

  override def mostPlayed(user: User): Future[Seq[MostPlayedEntry]] = {
    val query = playbackHistory(user).groupBy {
      case (record, track) => track
    }.map {
      case (track, rs) => (track, rs.length)
    }.sortBy {
      case (track, count) => count.desc
    }
    runQuery(query).map(_.map {
      case (track, count) => PimpMostPlayedEntry(track, count)
    })
  }

  private def playbackHistory(user: User) = for {
    record <- plays if record.who === user
    track <- tracks if track.id === record.track
  } yield (record, track)
}
