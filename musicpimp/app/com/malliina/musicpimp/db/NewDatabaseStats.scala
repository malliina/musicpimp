package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularEntry, RecentEntry}
import com.malliina.values.Username
import io.getquill.*
import scala.concurrent.Future

object NewDatabaseStats:
  def apply(db: PimpMySQL): NewDatabaseStats = new NewDatabaseStats(db)

class NewDatabaseStats(val db: PimpMySQL) extends PlaybackStats:
  import db.*

  private val playsTable = quote(querySchema[PlaybackRecord]("PLAYS"))
  private def playbackHistory(user: Username) = quote:
    for
      record <- playsTable if record.who == lift(user)
      track <- tracksTable if track.id == record.track
    yield TrackRecord(record, track)

  override def played(track: TrackMeta, user: Username): Future[Unit] =
    performAsync(s"Save played track ${track.title} (${track.id}) by user $user"):
      val now = Instant.now()
      implicit val enc: db.JdbcEncoder[Instant] = db.ie
      run(playsTable.insertValue(lift(PlaybackRecord(track.id, now, user))))

  def mostRecent(request: DataRequest): Future[Seq[RecentEntry]] =
    performAsync(s"Most recent tracks by ${request.username}"):
      implicit val dec: db.JdbcDecoder[Instant] = db.id
      run(
        playbackHistory(request.username)
          .sortBy(_.record.started)(Ord.desc)
          .drop(lift(request.from))
          .take(lift(request.maxItems))
      ).map: r =>
        RecentEntry(r.track, r.record.started)

  def mostPlayed(request: DataRequest): Future[Seq[PopularEntry]] =
    performAsync(s"Most played tracks by ${request.username}"):
      implicit val dec: db.JdbcDecoder[Instant] = db.id
      run(
        playbackHistory(request.username)
          .groupBy(_.track)
          .map:
            case (track, rs) => (track, rs.size, rs.map(_.record.started).min)
          .sortBy { case (_, count, date) => (count, date) }(Ord.desc)
          .map:
            case (track, count, _) => (track, count)
          .drop(lift(request.from))
          .take(lift(request.maxItems))
      ).map:
        case (track, count) => PopularEntry(track, count.toInt)
