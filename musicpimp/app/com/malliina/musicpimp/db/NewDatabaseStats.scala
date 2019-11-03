package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularEntry, RecentEntry}
import com.malliina.values.Username

import scala.concurrent.Future

object NewDatabaseStats {
  def apply(db: PimpMySQL): NewDatabaseStats = new NewDatabaseStats(db)
}

class NewDatabaseStats(val db: PimpMySQL) extends PlaybackStats {
  import db._

  val playsTable = quote(querySchema[PlaybackRecord]("PLAYS"))
  val playbackHistory = quote { user: Username =>
    for {
      record <- playsTable if record.who == user
      track <- tracksTable if track.id == record.track
    } yield TrackRecord(record, track)
  }

  override def played(track: TrackMeta, user: Username): Future[Unit] =
    performAsync("Save played track") {
      runIO(playsTable.insert(lift(PlaybackRecord(track.id, Instant.now(), user)))).map(_ => ())
    }

  def mostRecent(request: DataRequest): Future[Seq[RecentEntry]] = performAsync("Most recent") {
    runIO(
      playbackHistory(lift(request.username))
        .sortBy(_.record.started)(Ord.desc)
        .drop(lift(request.from))
        .take(lift(request.maxItems))
    ).map { recs =>
      recs.map { r =>
        RecentEntry(r.track, r.record.started)
      }
    }
  }

  def mostPlayed(request: DataRequest): Future[Seq[PopularEntry]] = performAsync("Most played") {
    runIO(
      playbackHistory(lift(request.username))
        .groupBy(_.track)
        .map { case (track, rs) => (track, rs.size, rs.map(_.record.started).min) }
        .sortBy { case (_, count, date) => (count, date) }(Ord.desc)
        .map { case (track, count, _) => (track, count) }
        .drop(lift(request.from))
        .take(lift(request.maxItems))
    ).map { rows =>
      rows.map { case (track, count) => PopularEntry(track, count.toInt) }
    }
  }
}
