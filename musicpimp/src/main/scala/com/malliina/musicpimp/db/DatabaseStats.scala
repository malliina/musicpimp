package com.malliina.musicpimp.db

import cats.effect.Async
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularEntry, RecentEntry}
import com.malliina.values.Username
import doobie.implicits.*

import java.time.Instant

class DatabaseStats[F[_]: Async](db: DoobieDatabase[F])
  extends PlaybackStats[F]
  with DoobieMappings:

  def played(track: TrackMeta, user: Username): F[Unit] = db.run:
    val now: Instant = Instant.now()
    sql"""insert into PLAYS(TRACK, STARTED, WHO) values (${track.id}, $now, $user)""".update.run
      .map(_ => ())

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
