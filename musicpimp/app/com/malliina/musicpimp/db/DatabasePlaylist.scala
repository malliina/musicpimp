package com.malliina.musicpimp.db

import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist}
import com.malliina.values.Username

import scala.concurrent.{ExecutionContext, Future}

class DatabasePlaylist(val db: PimpDb) extends Sessionizer(db) with PlaylistService {
  implicit val trackMapping = db.mappings.trackId
  implicit val userMapping = db.mappings.username
  import db.schema._
  import db.api._

  override implicit def ec: ExecutionContext = db.ec

  override protected def playlists(user: Username): Future[Seq[SavedPlaylist]] = {
    val query = playlistQuery(playlistsTable.filter(_.user === user))
    runQuery(query).map(collectPlaylists)
  }

  override protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] = {
    val q = playlistQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id.id))
    runQuery(q).map(collectPlaylists).map(_.headOption)
  }

  private def collectPlaylists(rows: Seq[(Long, String, Option[(DataTrack, Int)])]): Seq[SavedPlaylist] = {
    rows.foldLeft(Vector.empty[SavedPlaylist]) { case (acc, (id, name, link)) =>
      val idx = acc.indexWhere(_.id.id == id)
      if (idx >= 0) {
        val old = acc(idx)
        link.fold(acc) { l => acc.updated(idx, old.copy(tracks = old.tracks :+ l._1)) }
      } else {
        acc :+ SavedPlaylist(PlaylistID(id), name, link.map(_._1).toSeq)
      }
    }
  }

  override protected def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: Username): Future[PlaylistID] = {
    val owns = playlist.id.map(ownsPlaylist(_, user)).getOrElse(Future.successful(true))
    owns flatMap { isOwner =>
      if (isOwner) {
        def insertionQuery = (playlistsTable returning playlistsTable.map(_.id))
          .into((item, id) => item.copy(id = Option(id))) += PlaylistRow(None, playlist.name, user)

        def newPlaylistId: Future[Long] = db.database.run(insertionQuery).map(_.id.getOrElse(0L))

        val playlistId: Future[Long] = playlist.id.map(plid => Future.successful(plid.id)).getOrElse(newPlaylistId)
        playlistId.flatMap { id =>
          val entries = playlist.tracks.zipWithIndex.map {
            case (track, index) => PlaylistTrack(id, track, index)
          }
          val deletion = playlistTracksTable.filter(link => link.playlist === id && !link.idx.inSet(entries.map(_.index))).delete
          val action = DBIO.sequence(entries.map(entry => playlistTracksTable.insertOrUpdate(entry)) ++ Seq(deletion))
          run(action.transactionally).map(_ => PlaylistID(id))
        }
      } else {
        Future.failed(new UnauthorizedException(s"User $user is unauthorized"))
      }
    }
  }

  override def delete(id: PlaylistID, user: Username): Future[Unit] =
    run(playlistsTable.filter(pl => pl.user === user && pl.id === id.id).delete).map(_ => ())

  protected def ownsPlaylist(id: PlaylistID, user: Username): Future[Boolean] =
    runQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id.id)).map(_.nonEmpty)

  private def playlistQuery(lists: Query[PlaylistTable, PlaylistTable#TableElementType, Seq]) =
    lists.joinLeft(playlistTracksTable.join(tracks).on(_.track === _.id)).on(_.id === _._1.playlist)
      .map { case (pl, maybeLink) => (pl.id, pl.name, maybeLink.map(l => (l._2, l._1.idx))) }
      .sortBy { case (id, name, maybeLink) => (name.asc, id, maybeLink.map({ case (_, idx) => idx }).asc.nullsLast) }
}
