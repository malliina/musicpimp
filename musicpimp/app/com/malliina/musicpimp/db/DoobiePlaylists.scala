package com.malliina.musicpimp.db

import cats.effect.Async
import cats.implicits.{toFunctorOps, toTraverseOps}
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.db.DoobiePlaylists.{collect, log}
import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import doobie.free.connection.{pure, raiseError}
import doobie.implicits.{deriveRead, toSqlInterpolator}
import doobie.util.fragments

object DoobiePlaylists:
  private val log = AppLogger(getClass)

  def collect(rows: List[PlaylistInfo]): Seq[SavedPlaylist] =
    rows.foldLeft(Vector.empty[SavedPlaylist]): (acc, row) =>
      val idx = acc.indexWhere(_.id == row.id)
      if idx >= 0 then
        val old = acc(idx)
        row.track.fold(acc): t =>
          acc.updated(idx, old.copy(tracks = old.tracks :+ t))
      else
        acc :+ SavedPlaylist(
          row.id,
          row.name,
          row.trackCount,
          row.duration,
          row.track.toList
        )

class DoobiePlaylists[F[_]: Async](db: DoobieDatabase[F])
  extends PlaylistService[F]
  with DoobieMappings:
  def playlists(user: Username): F[Seq[SavedPlaylist]] =
    loadPlaylists(user, None)

  def playlist(id: PlaylistID, user: Username): F[Option[SavedPlaylist]] =
    loadPlaylists(user, Option(id)).map(_.headOption)

  def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: Username): F[PlaylistID] =
    db.run:
      val owns = playlist.id.fold(pure(true)): id =>
        sql"""select exists(select ID from PLAYLISTS where USER = $user and ID = $id)"""
          .query[Boolean]
          .unique
      owns.flatMap: allowed =>
        val indexedTracks = playlist.tracks.zipWithIndex
        if allowed then
          val idxCondition =
            fragments.notInOpt(fr"IDX", indexedTracks.map(_._2)).getOrElse(fr"true")
          for
            id <- playlist.id
              .map(pid => pure(pid))
              .getOrElse:
                sql"insert into PLAYLISTS(NAME, USER) values (${playlist.name}, $user)".update
                  .withUniqueGeneratedKeys[PlaylistID]("ID")
            _ <- sql"delete from PLAYLIST_TRACKS where PLAYLIST = $id and $idxCondition".update.run
            updateCount <- indexedTracks.toList.traverse: (t, idx) =>
              sql"select not exists(select TRACK from PLAYLIST_TRACKS PT where PT.PLAYLIST = $id and PT.IDX = $idx)"
                .query[Boolean]
                .unique
                .flatMap: isEmpty =>
                  val frag =
                    if isEmpty then
                      sql"insert into PLAYLIST_TRACKS(PLAYLIST, TRACK, IDX) values($id, ${t.id}, $idx)"
                    else
                      sql"update PLAYLIST_TRACKS set TRACK = ${t.id} where PLAYLIST = $id and IDX = $idx"
                  frag.update.run
          yield id
        else raiseError(UnauthorizedException(s"User $user is unauthorized"))

  def delete(id: PlaylistID, user: Username): F[Unit] = db.run:
    sql"""delete from PLAYLISTS where USER = $user and ID = $id""".update.run.map: rowsDeleted =>
      if rowsDeleted != 0 then
        log.info(s"Deleted playlist $id by '$user'. $rowsDeleted affected rows.")
      else log.info(s"Attempted to delete playlist $id by '$user', but no rows were affected.")

  private def loadPlaylists(user: Username, id: Option[PlaylistID]): F[Seq[SavedPlaylist]] = db.run:
    val idCondition = id.fold(fr"true")(pid => fr"P.ID = $pid")
    sql"""select P.ID, P.NAME, ifnull(AGG.TRACK_COUNT, 0), ifnull(AGG.LIST_DURATION, 0), T.ID, T.TITLE, T.ARTIST, T.ALBUM, T.DURATION, T.SIZE, T.PATH, T.FOLDER, PT.IDX
          from PLAYLISTS P
          left join PLAYLIST_TRACKS PT on P.ID = PT.PLAYLIST
          left join TRACKS T on PT.TRACK = T.ID
          left join (select PT.PLAYLIST, count(T.ID) as TRACK_COUNT, sum(T.DURATION) as LIST_DURATION
                     from PLAYLIST_TRACKS PT join TRACKS T on PT.TRACK = T.ID
                     group by PT.PLAYLIST) AGG on AGG.PLAYLIST = P.ID
          where P.USER = $user and $idCondition
          order by P.ID, PT.IDX"""
      .query[PlaylistInfo]
      .to[List]
      .map: rows =>
        collect(rows)
