package com.malliina.musicpimp.db

import cats.effect.Async
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularEntry, RecentEntry}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import doobie.implicits.toSqlInterpolator
import DatabaseStats.log
import java.time.Instant

object DatabaseStats:
  private val log = AppLogger(getClass)

class DatabaseStats[F[_]: Async](db: DoobieDatabase[F])
  extends PlaybackStats[F]
  with DoobieMappings:

  def playedTrack(track: TrackID, user: Username, when: Instant): F[Unit] = db.run:
    sql"""insert into PLAYS(TRACK, STARTED, WHO) values ($track, $when, $user)""".update.run
      .map(_ => log.info(s"Added $track by $user to play history."))

  def mostRecent(request: DataRequest): F[List[RecentEntry]] = db.run:
    sql"""select T.ID, T.TITLE, T.ARTIST, T.ALBUM, T.DURATION, T.SIZE, T.PATH, T.FOLDER, P.STARTED
          from PLAYS P join TRACKS T ON P.TRACK = T.ID
          where P.WHO = ${request.username} order by P.STARTED desc
          limit ${request.maxItems} offset ${request.from}"""
      .query[RecentEntry]
      .to[List]

  def mostPlayed(request: DataRequest): F[List[PopularEntry]] = db.run:
    sql"""select T.ID, T.TITLE, T.ARTIST, T.ALBUM, T.DURATION, T.SIZE, T.PATH, T.FOLDER, count(*)
          from PLAYS P join TRACKS T ON P.TRACK = T.ID
          where P.WHO = ${request.username}
          group by T.ID
          order by count(*) desc, min(P.STARTED) desc
          limit ${request.maxItems} offset ${request.from}"""
      .query[PopularEntry]
      .to[List]
