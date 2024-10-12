package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.NewPlaylist.collectPlaylists
import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist}
import com.malliina.values.Username
import io.getquill.*
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

object NewPlaylist:
  def apply(db: PimpMySQL): NewPlaylist = new NewPlaylist(db)

  private def collectPlaylists(rows: Seq[NewPlaylistInfo]): Seq[SavedPlaylist] =
    rows.foldLeft(Vector.empty[SavedPlaylist]):
      case (acc, row) =>
        val idx = acc.indexWhere(_.id == row.id)
        if idx >= 0 then
          val old = acc(idx)
          row.track.fold(acc): l =>
            acc.updated(idx, old.copy(tracks = old.tracks :+ l.track))
        else
          acc :+ SavedPlaylist(
            row.id,
            row.name,
            row.trackCount.toInt,
            row.duration,
            row.track.map(_.track).toSeq
          )

class NewPlaylist(val db: PimpMySQL) extends PlaylistService:
  override implicit val ec: ExecutionContext = db.ec
  import db.*

  val playlistsTable = quote(querySchema[PlaylistRecord]("PLAYLISTS"))
  val playlistTracksTable = quote(querySchema[PlaylistTrack]("PLAYLIST_TRACKS"))

  val linked = quote:
    for
      pt <- playlistTracksTable
      t <- tracksTable if pt.track == t.id
    yield PlayPair(t, pt)
  val aggregates = quote:
    linked
      .groupBy(_.link.playlist)
      .map:
        case (id, group) =>
          PlaylistTotals(
            id,
            group.size,
            group.map(_.track.duration).sum.getOrElse(lift(Duration.Zero))
          )
  val indexed = quote:
    for
      pl <- playlistsTable
      pt <- playlistTracksTable if pl.id == pt.playlist
      t <- tracksTable if pt.track == t.id
    yield IndexedPlaylistTrack(pl.id, t, pt.idx)
  val playlistQuery = quote: (q: Query[PlaylistRecord]) =>
    for
      pl <- q
      totals <- aggregates.leftJoin(a => a.id == pl.id)
      it <- indexed.leftJoin(it => it.playlist == pl.id)
    yield NewPlaylistInfo(
      pl.id,
      pl.name,
      totals.map(_.tracks).getOrElse(lift(0L)),
      totals.map(_.duration).getOrElse(lift(Duration.Zero)),
      it
    )

  protected def playlists(user: Username): Future[Seq[SavedPlaylist]] =
    performAsync(s"Load playlists by $user"):
      val rows = run(playlistQuery(playlistsTable.filter(pl => pl.user == lift(user))))
      collectPlaylists(rows)

  /** @param id
    *   playlist id
    * @return
    *   the playlist with ID `id`, or None if no such playlist exists
    */
  protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] =
    performAsync(s"Load playlist $id by $user"):
      val rows =
        run(playlistQuery(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id))))
      collectPlaylists(rows).headOption

  /** @param playlist
    *   playlist submission
    * @return
    *   a Future that completes when saving is done
    */
  protected def saveOrUpdatePlaylist(
    playlist: PlaylistSubmission,
    user: Username
  ): Future[PlaylistID] = transactionally(s"Save playlist by $user"):
    val owns = playlist.id.forall: id =>
      run(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id))).nonEmpty
    val idTask = playlist.id
      .map: id =>
        id
      .getOrElse:
        run(
          playlistsTable
            .insert(_.name -> lift(playlist.name), _.user -> lift(user))
            .returningGenerated(_.id)
        )
    if owns then updatePlaylist(idTask, playlist)
    else throw new UnauthorizedException(s"User $user is unauthorized")

  private def updatePlaylist(id: PlaylistID, playlist: PlaylistSubmission) =
    val entries = playlist.tracks.zipWithIndex.map:
      case (track, index) => IndexedTrackId(track, index)
    val deletion = run(
      playlistTracksTable
        .filter: link =>
          link.playlist == lift(id) && !liftQuery(entries.map(_.idx)).contains(link.idx)
        .delete
    )
    val updates = entries.map: e =>
      val isEmpty = run(
        playlistTracksTable
          .filter(pl => pl.playlist == lift(id) && pl.idx == lift(e.idx))
          .isEmpty
      )
      if isEmpty then
        run(
          playlistTracksTable
            .insert(_.playlist -> lift(id), _.track -> lift(e.track), _.idx -> lift(e.idx))
        )
      else
        run(
          playlistTracksTable
            .filter(pl => pl.playlist == lift(id) && pl.idx == lift(e.idx))
            .update(_.track -> lift(e.track))
        )
    id

  def delete(id: PlaylistID, user: Username): Future[Unit] =
    performAsync(s"Delete playlist $id by $user"):
      run(playlistsTable.filter(pl => pl.user == lift(user) && pl.id == lift(id)).delete)
