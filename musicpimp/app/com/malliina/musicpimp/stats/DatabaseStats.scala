package com.malliina.musicpimp.stats

import java.time.Instant

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db._
import com.malliina.values.Username

import scala.concurrent.{ExecutionContext, Future}

class DatabaseStats(val db: PimpDb)(implicit ec: ExecutionContext)
  extends Sessionizer(db)
    with PlaybackStats {

  import db.schema._
  import db.api._

  override def played(track: TrackMeta, user: Username): Future[Unit] =
    run(plays += PlaybackRecord(track.id, Instant.now, user)).map(_ => ())

  override def mostRecent(request: DataRequest): Future[Seq[RecentEntry]] = {
    val sortedHistory = playbackHistory(request.username)
      .sortBy(_._1.when.desc)
      .drop(request.from)
      .take(request.maxItems)
    runQuery(sortedHistory).map { rows =>
      rows.map {
        case (record, track) => RecentEntry(track, record.when)
      }
    }
  }

  override def mostPlayed(request: DataRequest): Future[Seq[PopularEntry]] = {
    val query = playbackHistory(request.username)
      .groupBy { case (_, track) => track }
      .map { case (track, rs) => (track, (rs.length, rs.map(_._1.when).min.getOrElse(Instant.now()))) }
      .sortBy { case (_, (count, date)) => (count.desc, date.desc) }
      .map { case (track, (count, _)) => (track, count) }
      .drop(request.from)
      .take(request.maxItems)
    runQuery(query).map { rows =>
      rows.map {
        case (track, count) => PopularEntry(track, count)
      }
    }
  }

  private def playbackHistory(user: Username) = for {
    record <- plays if record.who === user
    track <- tracks if track.id === record.track
  } yield (record, track)
}
