package com.malliina.musicpimp.db

import com.malliina.musicpimp.exception.UnauthorizedException
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, SavedPlaylist}
import com.malliina.values.Username
import io.getquill.Embedded

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
case class IndexedTrack(track: DataTrack, index: Int) extends Embedded
case class IndexedPlaylistTrack(playlist: PlaylistID, track: DataTrack, index: Int) extends Embedded

case class PlaylistInfo(
  id: PlaylistID,
  name: String,
  trackCount: Int,
  duration: FiniteDuration,
  track: Option[IndexedTrack]
) extends Embedded

case class PlaylistInfo2(
  id: PlaylistID,
  name: String,
  trackCount: Long,
  duration: FiniteDuration,
  track: Option[IndexedPlaylistTrack]
) extends Embedded

class DatabasePlaylist(val db: PimpDb) extends Sessionizer(db) with PlaylistService {

  import db.api._
  import db.schema._

  val zero: Rep[FiniteDuration] = Duration.Zero.bind

  case class IndexedTrackRep(track: DataTrackRep, index: Rep[Int])

  implicit object IndexedShape extends CaseClassShape(IndexedTrackRep.tupled, IndexedTrack.tupled)

  case class PlaylistInfoRep(
    id: Rep[PlaylistID],
    name: Rep[String],
    trackCount: Rep[Int],
    duration: Rep[FiniteDuration],
    track: Rep[Option[IndexedTrackRep]]
  )

  implicit object PlaylistInfoShape
    extends CaseClassShape(PlaylistInfoRep.tupled, PlaylistInfo.tupled)

  override implicit def ec: ExecutionContext = db.ec

  override protected def playlists(user: Username): Future[Seq[SavedPlaylist]] = {
    val query = playlistQuery(playlistsTable.filter(_.user === user))
    runQuery(query).map(collectPlaylists)
  }

  override protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] = {
    val q = playlistQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id))
    runQuery(q).map(collectPlaylists).map(_.headOption)
  }

  private def collectPlaylists(rows: Seq[PlaylistInfo]): Seq[SavedPlaylist] =
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
            row.trackCount,
            row.duration,
            row.track.map(_.track).toSeq
          )
        }
    }

  override protected def saveOrUpdatePlaylist(
    playlist: PlaylistSubmission,
    user: Username
  ): Future[PlaylistID] = {
    val owns = playlist.id.map(ownsPlaylist(_, user)).getOrElse(Future.successful(true))
    owns flatMap { isOwner =>
      if (isOwner) {
        def insertionQuery = (playlistsTable returning playlistsTable.map(_.id))
          .into((item, id) => item.copy(id = Option(id))) += PlaylistRow(None, playlist.name, user)

        def newPlaylistId: Future[PlaylistID] =
          db.database.run(insertionQuery).map(_.id.getOrElse(PlaylistID(0L)))

        val playlistId: Future[PlaylistID] =
          playlist.id.map(plid => Future.successful(plid)).getOrElse(newPlaylistId)
        playlistId.flatMap { id =>
          val entries = playlist.tracks.zipWithIndex.map {
            case (track, index) => PlaylistTrack(id, track, index)
          }
          val deletion = playlistTracksTable
            .filter(link => link.playlist === id && !link.idx.inSet(entries.map(_.idx)))
            .delete
          val action = DBIO.sequence(
            entries.map(entry => playlistTracksTable.insertOrUpdate(entry)) ++ Seq(deletion)
          )
          run(action.transactionally).map(_ => id)
        }
      } else {
        Future.failed(new UnauthorizedException(s"User $user is unauthorized"))
      }
    }
  }

  override def delete(id: PlaylistID, user: Username): Future[Unit] =
    run(playlistsTable.filter(pl => pl.user === user && pl.id === id).delete).map(_ => ())

  protected def ownsPlaylist(id: PlaylistID, user: Username): Future[Boolean] =
    runQuery(playlistsTable.filter(pl => pl.user === user && pl.id === id)).map(_.nonEmpty)

  val playlistAggregates =
    playlistTracksTable.join(tracks).on(_.track === _.id).groupBy(_._1.playlist).map {
      case (id, q) => (id, q.length, q.map(_._2.duration).sum.getOrElse(zero))
    }

  private def playlistQuery(
    lists: Query[PlaylistTable, PlaylistTable#TableElementType, Seq]
  ): Query[PlaylistInfoRep, PlaylistInfo, Seq] =
    lists
      .joinLeft(playlistAggregates)
      .on(_.id === _._1)
      .joinLeft(playlistTracksTable.join(tracks).on(_.track === _.id))
      .on(_._1.id === _._1.playlist)
      .map {
        case ((pl, agg), maybeLink) =>
          PlaylistInfoRep(
            pl.id,
            pl.name,
            agg.map(_._2).getOrElse(0),
            agg.map(_._3).getOrElse(zero),
            maybeLink.map(l => IndexedTrackRep(l._2.projection, l._1.idx))
          )
      }
      .sortBy { t =>
        (t.name.asc, t.id, t.track.map(_.index).asc.nullsLast)
      }
}
