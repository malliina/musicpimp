package com.malliina.musicpimp.db

import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.TrackIDs.trackData
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist}
import com.malliina.play.models.Username
import slick.jdbc.H2Profile.api._
import slick.lifted.Query

import scala.concurrent.{ExecutionContext, Future}

class DatabasePlaylist(db: PimpDb) extends Sessionizer(db) with PlaylistService {
  implicit val userMapping = com.malliina.musicpimp.db.Mappings.username

  import PimpSchema.{playlistTracksTable, playlistsTable, tracks}

  override implicit def ec: ExecutionContext = db.ec

  override protected def playlists(user: Username): Future[Seq[SavedPlaylist]] = {
    val query = playlistQuery(playlistsTable.filter(_.user === user))
    runQuery(query).map(collectPlaylists)
  }

  override protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] = {
    val q = playlistQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id.id))
    val result = runQuery(q.sortBy(_._4))
    result.map(collectPlaylists).map(_.headOption)
  }

  private def collectPlaylists(rows: Seq[(Long, String, DataTrack, Int)]) = {
    rows.foldLeft(Vector.empty[SavedPlaylist]) { case (acc, (id, name, track, _)) =>
      val idx = acc.indexWhere(_.id.id == id)
      if (idx >= 0) {
        val old = acc(idx)
        acc.updated(idx, old.copy(tracks = old.tracks :+ track))
      } else {
        acc :+ SavedPlaylist(PlaylistID(id), name, Seq(track))
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

  private def playlistQuery(lists: Query[PlaylistTable, PlaylistTable#TableElementType, Seq]) = {
    val joined = for {
      pls <- lists
      pts <- playlistTracksTable if pts.playlist === pls.id
      ts <- tracks if ts.id === pts.track
    } yield {
      (pls.id, pls.name, ts, pts.idx)
    }
    joined.sortBy { case (id, name, _, index) => (name.asc, id, index.asc) }
  }
}
