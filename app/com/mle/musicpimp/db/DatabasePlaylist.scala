package com.mle.musicpimp.db

import com.mle.concurrent.ExecutionContexts.cached
import com.mle.musicpimp.exception.UnauthorizedException
import com.mle.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.mle.musicpimp.models.{PlaylistID, SavedPlaylist, User}
import com.mle.util.Log

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._
import scala.slick.lifted.Query

/**
  * @author mle
  */
class DatabasePlaylist(db: PimpDatabase) extends PlaylistService with Log {

  import db._

  override protected def playlists(user: User): Future[Seq[SavedPlaylist]] = {
    val result = withSession(s => {
      val baseQuery = playlistsTable.filter(_.user === user.name)
      val q = playlistQuery(baseQuery)
      q.run(s)
    })
    result.map(data => {
      data.map((PlaylistEntry.apply _).tupled)
        .groupBy(_.id)
        .flatMap(kv => toPlaylist(kv._2))
        .toList
    })
  }

  override protected def playlist(id: PlaylistID, user: User): Future[Option[SavedPlaylist]] = {
    val result = withSession(s => {
      val q = playlistQuery(playlistsTable.filter(pl => pl.user === user.name && pl.id === id.id))
      q.sortBy(_._4).run(s)
    })
    result.map(data => {
      val es = data.map((PlaylistEntry.apply _).tupled)
      toPlaylist(es)
    })
  }

  override protected def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[PlaylistID] = {
    val owns = playlist.playlistId.map(ownsPlaylist(_, user)).getOrElse(Future.successful(true))
    owns.flatMap(isOwner => {
      if (isOwner) {
        withSession(implicit s => {
          def insertionQuery = (playlistsTable returning playlistsTable.map(_.id)) += PlaylistRow(None, playlist.name, user)
          val id: Long = playlist.playlistId.map(_.id).getOrElse(insertionQuery.run(s))
          val entries = playlist.tracks.zipWithIndex.map {
            case (track, index) => PlaylistTrack(id, track, index)
          }
          entries.map(entry => playlistTracksTable.insertOrUpdate(entry)(s))
          PlaylistID(id)
        })
      } else {
        Future.failed(new UnauthorizedException(s"User $user is unauthorized"))
      }
    })
  }

  override def delete(id: PlaylistID, user: User): Future[Unit] = {
    withSession(s => {
      playlistsTable.filter(pl => pl.user === user.name && pl.id === id.id).delete(s)
    }).map(_ => ())
  }

  // transient class
  case class PlaylistEntry(id: Long, name: String, track: DataTrack, index: Int)

  protected def ownsPlaylist(id: PlaylistID, user: User): Future[Boolean] = {
    withSession(s => playlistsTable.filter(pl => pl.user === user.name && pl.id === id.id).exists.run(s))
  }

  private def playlistQuery(lists: Query[PlaylistTable, PlaylistTable#TableElementType, scala.Seq]) = {
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
