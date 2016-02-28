package com.malliina.musicpimp.db

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist, User}
import com.malliina.util.Log
import slick.driver.H2Driver.api._
import slick.lifted.Query

import scala.concurrent.Future

class DatabasePlaylist(db: PimpDb) extends Sessionizer(db) with PlaylistService with Log {

  import PimpSchema.{playlistTracksTable, playlistsTable, tracks}

  override protected def playlists(user: User): Future[Seq[SavedPlaylist]] = {
    runQuery(playlistQuery(playlistsTable.filter(_.user === user))).map(data => {
      data.map((PlaylistEntry.apply _).tupled)
        .groupBy(_.id)
        .flatMap(kv => toPlaylist(kv._2))
        .toList
    })
  }

  override protected def playlist(id: PlaylistID, user: User): Future[Option[SavedPlaylist]] = {
    val q = playlistQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id.id))
    val result = runQuery(q.sortBy(_._4))
    result.map(data => {
      val es = data.map((PlaylistEntry.apply _).tupled)
      toPlaylist(es)
    })
  }

  override protected def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[PlaylistID] = {
    val owns = playlist.playlistId.map(ownsPlaylist(_, user)).getOrElse(Future.successful(true))
    owns.flatMap(isOwner => {
      if (isOwner) {
        def insertionQuery = (playlistsTable returning playlistsTable.map(_.id))
          .into((item, id) => item.copy(id = Option(id))) += PlaylistRow(None, playlist.name, user)
        def newPlaylistId: Future[Long] = db.database.run(insertionQuery).map(_.id.getOrElse(0L))
        val playlistId: Future[Long] = playlist.playlistId.map(plid => Future.successful(plid.id)).getOrElse(newPlaylistId)
        playlistId.flatMap { id =>
          val entries = playlist.tracks.zipWithIndex.map {
            case (track, index) => PlaylistTrack(id, track, index)
          }
          val action = DBIO.sequence(entries.map(entry => playlistTracksTable.insertOrUpdate(entry)))
          run(action).map(_ => PlaylistID(id))
        }
      } else {
        Future.failed(new UnauthorizedException(s"User $user is unauthorized"))
      }
    })
  }

  override def delete(id: PlaylistID, user: User): Future[Unit] =
    run(playlistsTable.filter(pl => pl.user === user && pl.id === id.id).delete).map(_ => ())

  // transient class
  case class PlaylistEntry(id: Long, name: String, track: DataTrack, index: Int)

  protected def ownsPlaylist(id: PlaylistID, user: User): Future[Boolean] =
    runQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id.id)).map(_.nonEmpty)

  private def playlistQuery(lists: Query[PlaylistTable, PlaylistTable#TableElementType, Seq]) = {
    for {
      pls <- lists
      pts <- playlistTracksTable if pts.playlist === pls.id
      ts <- tracks if ts.id === pts.track
    } yield {
      (pls.id, pls.name, ts, pts.idx)
    }
  }

  private def toPlaylist(es: Seq[PlaylistEntry]): Option[SavedPlaylist] =
    es.headOption.map(e => SavedPlaylist(PlaylistID(e.id), e.name, es.sortBy(_.index).map(_.track)))
}
