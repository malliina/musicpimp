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

  override def mostRecent(request: DataRequest): Future[Seq[RecentEntry]] = {
    val sortedHistory = playbackHistory(request.username)
      .sortBy(_._1.when.desc)
      .drop(request.from)
      .take(request.maxItems)
    runQuery(sortedHistory).map(_.map {
      case (record, track) => RecentEntry(track, record.when)
    })
  }

  override def mostPlayed(request: DataRequest): Future[Seq[PopularEntry]] = {
    val query = playbackHistory(request.username)
      .groupBy { case (record, track) => track }
      .map { case (track, rs) => (track, (rs.length, rs.map(_._1.when).min.getOrElse(DateTime.now()))) }
      .sortBy { case (track, (count, date)) => (count.desc, date.desc) }
      .map { case (track, (count, date)) => (track, count) }
      .drop(request.from)
      .take(request.maxItems)
    runQuery(query).map(_.map {
      case (track, count) => PopularEntry(track, count)
    })
  }

  private def playbackHistory(user: User) = for {
    record <- plays if record.who === user
    track <- tracks if track.id === record.track
  } yield (record, track)
}
