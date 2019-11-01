package com.malliina.musicpimp.db

import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist, TrackID}
import com.malliina.values.Username

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import NewPlaylist.collectPlaylists
import com.malliina.musicpimp.exception.UnauthorizedException

object NewPlaylist {
  private def collectPlaylists(rows: Seq[PlaylistInfo2]): Seq[SavedPlaylist] =
    rows.foldLeft(Vector.empty[SavedPlaylist]) {
      case (acc, row) =>
        val idx = acc.indexWhere(_.id == row.id)
        if (idx >= 0) {
          val old = acc(idx)
          row.track.fold(acc) { l =>
            acc.updated(idx, old.copy(tracks = old.tracks :+ l.track))
          }
        } else {
          acc :+ SavedPlaylist(
            row.id,
            row.name,
            row.trackCount.toInt,
            row.duration,
            row.track.map(_.track).toSeq
          )
        }
    }
}

case class PlayPair(track: DataTrack, link: PlaylistTrack)
case class IndexedTrackId(track: TrackID, idx: Int)

class NewPlaylist(val db: PimpMySQL) extends PlaylistService {
  import db._

  val playlistsTable = quote(querySchema[PlaylistRecord]("PLAYLISTS"))
  // The _.idx trick works around buggy aliasing due to INDEX being a keyword in MySQL
  val playlistTracksTable = quote(querySchema[PlaylistTrack]("PLAYLIST_TRACKS", _.idx -> "INDEX"))
  val tracksTable = quote(querySchema[DataTrack]("TRACKS"))

  val linked = quote {
    for {
      pt <- playlistTracksTable
      t <- tracksTable if pt.track == t.id
    } yield PlayPair(t, pt)
  }
  val aggregates = quote {
    linked.groupBy(_.link.playlist).map {
      case (id, group) =>
        PlaylistTotals(
          id,
          group.size,
          group.map(_.track.duration).sum.getOrElse(lift(Duration.Zero))
        )
    }
  }
  val indexed = quote {
    for {
      pl <- playlistsTable
      pt <- playlistTracksTable if pl.id == pt.playlist
      t <- tracksTable if pt.track == t.id
    } yield IndexedPlaylistTrack(pl.id, t, pt.idx)
  }
  val playlistQuery = quote { q: Query[PlaylistRecord] =>
    for {
      pl <- q
      totals <- aggregates.leftJoin(a => a.id == pl.id)
      it <- indexed.leftJoin(it => it.playlist == pl.id)
    } yield PlaylistInfo2(
      pl.id,
      pl.name,
      totals.map(_.tracks).getOrElse(lift(0L)),
      totals.map(_.duration).getOrElse(lift(Duration.Zero)),
      it
    )
  }

  protected def playlists(user: Username): Future[Seq[SavedPlaylist]] =
    performAsync("Load playlists") {
      runIO(playlistQuery(playlistsTable.filter(pl => pl.user == lift(user)))).map { rows =>
        collectPlaylists(rows)
      }
    }

  /**
    * @param id playlist id
    * @return the playlist with ID `id`, or None if no such playlist exists
    */
  protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] =
    performAsync(s"Load playlist $id by $user") {
      runIO(playlistQuery(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id)))).map {
        rows =>
          collectPlaylists(rows).headOption
      }
    }

  /**
    * @param playlist playlist submission
    * @return a Future that completes when saving is done
    */
  protected def saveOrUpdatePlaylist(
    playlist: PlaylistSubmission,
    user: Username
  ): Future[PlaylistID] = transactionally(s"Save playlist by $user") {
    val owns: IO[Boolean, Effect.Read] = playlist.id.map { id =>
      runIO(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id)).nonEmpty)
    }.getOrElse {
      IO.successful(true)
    }
    val idTask: IO[PlaylistID, Effect.Write] = playlist.id.map { id =>
      IO.successful(id)
    }.getOrElse {
      runIO(
        playlistsTable
          .insert(_.name -> lift(playlist.name), _.user -> lift(user))
          .returningGenerated(_.id)
      )
    }
    for {
      isOwner <- owns
      id <- if (isOwner) idTask.flatMap(id => updatePlaylist(id, playlist))
      else IO.failed(new UnauthorizedException(s"User $user is unauthorized"))
    } yield id
  }

  private def updatePlaylist(id: PlaylistID, playlist: PlaylistSubmission) = {
    val entries = playlist.tracks.zipWithIndex.map {
      case (track, index) => IndexedTrackId(track, index)
    }
    val deletion = runIO(playlistTracksTable.filter { link =>
      link.playlist == lift(id) && !liftQuery(entries.map(_.idx)).contains(link.idx)
    }.delete)
    val updates = IO.traverse(entries) { e =>
      runIO(
        playlistTracksTable
          .filter(pl => pl.playlist == lift(id) && pl.idx == lift(e.idx))
          .isEmpty
      ).flatMap { isEmpty =>
        if (isEmpty) {
          runIO(
            playlistTracksTable
              .insert(_.playlist -> lift(id), _.track -> lift(e.track), _.idx -> lift(e.idx))
          )
        } else {
          runIO(
            playlistTracksTable
              .filter(pl => pl.playlist == lift(id) && pl.idx == lift(e.idx))
              .update(_.track -> lift(e.track))
          )
        }
      }
    }
    for {
      d <- deletion
      us <- updates
    } yield id
  }

  def delete(id: PlaylistID, user: Username): Future[Unit] =
    performAsync(s"Delete playlist $id by $user") {
      runIO(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id)).delete)
        .map(_ => ())
    }
}
