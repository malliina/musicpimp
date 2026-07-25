package com.malliina.musicpimp.audio

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps, toTraverseOps}
import com.malliina.musicpimp.audio.PlaybackMessageHandler.log
import com.malliina.musicpimp.json.{JsonMessages, MediaRanges}
import com.malliina.musicpimp.library.{FileLibrary, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, RemoteInfo, TrackID}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import io.circe.Json
import io.circe.syntax.EncoderOps

class PlaybackMessageHandler[F[_]: Sync](
  player: MusicPlayer[F],
  library: FileLibrary,
  lib: MusicLibrary[F],
  statsPlayer: StatsPlayer[F]
) extends JsonHandlerBase:
  val F = Sync[F]
  def pure[T](t: T) = F.pure(t)

  val playlist = player.playlist

  def updateUser(user: Username): Unit = statsPlayer.updateUser(user)

  override def handleMessage(msg: Json, src: RemoteInfo[F]): F[Unit] =
    statsPlayer.updateUser(src.user)
    super.handleMessage(msg, src)

  override def fulfillMessage(message: PlayerMessage, src: RemoteInfo[F]): F[Unit] =
    message match
      case GetStatusMsg =>
        val json = src.apiVersion match
          case MediaRanges.JSONv17 => player.status17(src.host).map(_.asJson)
          case _                   => player.status(src.host).map(_.asJson)
        json.flatMap: s =>
          src.target.send(JsonMessages.withStatus(s))
      case ResumeMsg =>
        player.play()
      case StopMsg =>
        player.stop()
      case NextMsg =>
        player.nextTrack()
      case PrevMsg =>
        player.previousTrack()
      case MuteMsg(isMute) =>
        pure(player.mute(isMute))
      case VolumeMsg(vol) =>
        pure(player.volume(vol.volume))
      case SeekMsg(pos) =>
        pure(player.seek(pos))
      case PlayMsg(track) =>
        withTrack(track)(player.reset)
      case SkipMsg(index) =>
        player
          .skip(index)
          .handleError:
            case iae: IllegalArgumentException =>
              log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
      case AddMsg(track) =>
        withTrack(track)(playlist.add)
      case InsertTrackMsg(index, track) =>
        withTrack(track): t =>
          playlist.insert(index, t)
      case MoveTrackMsg(from, to) =>
        playlist.move(from, to)
      case RemoveMsg(index) =>
        playlist.delete(index)
      case AddAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks).map(_.foreach(playlist.add))
      case PlayAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks).map:
          case head :: tail =>
            player.reset(head)
            tail.foreach(playlist.add)
          case Nil =>
            log.warn(s"No tracks were resolved")
      case ResetPlaylistMessage(index, tracks) =>
        NonEmptyList
          .fromList(tracks.toList)
          .map: nel =>
            lib.tracks(nel)
          .getOrElse:
            Applicative[F].pure(Nil)
          .flatMap: ts =>
            playlist.reset(index, ts)
          .handleError:
            case e: Exception =>
              log.error("Unable to reset playlist.", e)
      case Handover(index, tracks, state, position) =>
        NonEmptyList
          .fromList(tracks.toList)
          .map: nel =>
            lib.tracks(nel)
          .getOrElse:
            Applicative[F].pure(Nil)
          .flatMap: ts =>
            playlist
              .reset(index.getOrElse(BasePlaylist.NoPosition), ts)
              .flatMap: _ =>
                playlist.current
                  .flatMap: opt =>
                    opt
                      .map: t =>
                        for
                          _ <- player.tryInitTrackWithFallback(t)
                          _ <- player.trySeek(position)
                          _ <- if PlayState.isPlaying(state) then player.play() else F.unit
                        yield ()
                      .getOrElse:
                        F.unit
              .handleError:
                case e: Exception =>
                  log.error("Handover failed.", e)
      case other =>
        F.delay:
          log.warn(s"Unsupported message: '$other'.")

  private def withTrack(id: TrackID)(code: LocalTrack => F[Unit]): F[Unit] =
    lib
      .meta(id)
      .flatMap: t =>
        t.map: local =>
          code(local)
        .getOrElse:
            F.delay(log.error(s"Track not found: '$id'."))
      .handleError:
        case e: Exception =>
          log.error(s"Track search failed.", e)

  private def resolveTracksOrEmpty(
    folders: Seq[FolderID],
    tracks: Seq[TrackID]
  ): F[List[LocalTrack]] =
    resolveTracks(folders, tracks)
      .map(_.toList)
      .handleError:
        case t: Exception =>
          log.error(
            s"Unable to resolve tracks from ${folders.size} folder and ${tracks.size} track references",
            t
          )
          Nil

  private def resolveTracks(folders: Seq[FolderID], tracks: Seq[TrackID]): F[Seq[LocalTrack]] =
    folders.toList
      .traverse: folder =>
        lib.tracksIn(folder).map(_.getOrElse(Nil)).map(library.localize)
      .map(_.flatten)
      .flatMap: subTracks =>
        NonEmptyList
          .fromList(tracks.toList)
          .map(nel => lib.tracks(nel))
          .getOrElse(F.pure(Nil))
          .map: lts =>
            lts ++ subTracks

object PlaybackMessageHandler:
  private val log = AppLogger(getClass)
