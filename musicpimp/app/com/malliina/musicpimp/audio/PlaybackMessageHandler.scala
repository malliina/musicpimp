package com.malliina.musicpimp.audio

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.toTraverseOps
import com.malliina.concurrent.Execution.runtime
import com.malliina.musicpimp.audio.PlaybackMessageHandler.log
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages}
import com.malliina.musicpimp.library.{FileLibrary, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, RemoteInfo, TrackID}
import com.malliina.values.Username
import io.circe.Json
import io.circe.syntax.EncoderOps
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PlaybackMessageHandler(
  player: MusicPlayer,
  library: FileLibrary,
  lib: MusicLibrary[IO],
  statsPlayer: StatsPlayer
)(implicit ec: ExecutionContext)
  extends JsonHandlerBase:

  val playlist = player.playlist

  def updateUser(user: Username): Unit = statsPlayer.updateUser(user)

  override def handleMessage(msg: Json, src: RemoteInfo): Unit =
    statsPlayer.updateUser(src.user)
    super.handleMessage(msg, src)

  override def fulfillMessage(message: PlayerMessage, src: RemoteInfo): Unit =
    message match
      case GetStatusMsg =>
        val json = src.apiVersion match
          case JsonFormatVersions.JSONv17 => player.status17(src.host).asJson
          case _                          => player.status(src.host).asJson
        src.target.send(JsonMessages.withStatus(json))
      case ResumeMsg =>
        player.play()
      case StopMsg =>
        player.stop()
      case NextMsg =>
        player.nextTrack()
      case PrevMsg =>
        player.previousTrack()
      case MuteMsg(isMute) =>
        player.mute(isMute)
      case VolumeMsg(vol) =>
        player.volume(vol.volume)
      case SeekMsg(pos) =>
        player.seek(pos)
      case PlayMsg(track) =>
        withTrack(track)(player.reset)
      case SkipMsg(index) =>
        Try(player.skip(index)).recover:
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
            log warn s"No tracks were resolved"
      case ResetPlaylistMessage(index, tracks) =>
        NonEmptyList
          .fromList(tracks.toList)
          .map: nel =>
            lib.tracks(nel).unsafeToFuture()
          .getOrElse:
            Future.successful(Nil)
          .map: ts =>
            playlist.reset(index, ts)
          .recover:
            case e: Exception =>
              log.error("Unable to reset playlist.", e)
      case Handover(index, tracks, state, position) =>
        NonEmptyList
          .fromList(tracks.toList)
          .map: nel =>
            lib.tracks(nel).unsafeToFuture()
          .getOrElse:
            Future.successful(Nil)
          .map: ts =>
            playlist.reset(index.getOrElse(BasePlaylist.NoPosition), ts)
            playlist.current.foreach: t =>
              for
                _ <- player.tryInitTrackWithFallback(t)
                _ <- player.trySeek(position)
              yield if PlayState.isPlaying(state) then player.play()
          .recover:
            case e: Exception =>
              log.error("Handover failed.", e)
      case other =>
        log.warn(s"Unsupported message: '$other'.")

  def withTrack(id: TrackID)(code: LocalTrack => Unit) =
    lib
      .meta(id)
      .map: t =>
        t.map: local =>
          code(local)
        .getOrElse:
            log.error(s"Track not found: '$id'.")
      .recover:
        case e: Exception =>
          log.error(s"Track search failed.", e)

  def resolveTracksOrEmpty(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[List[LocalTrack]] =
    resolveTracks(folders, tracks)
      .map(_.toList)
      .recover:
        case t: Exception =>
          log.error(
            s"Unable to resolve tracks from ${folders.size} folder and ${tracks.size} track references",
            t
          )
          Nil

  def resolveTracks(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[Seq[LocalTrack]] =
    folders.toList
      .traverse: folder =>
        lib.tracksIn(folder).map(_.getOrElse(Nil)).map(library.localize)
      .map(_.flatten)
      .flatMap: subTracks =>
        NonEmptyList
          .fromList(tracks.toList)
          .map(nel => lib.tracks(nel))
          .getOrElse(IO.pure(Nil))
          .map: lts =>
            lts ++ subTracks
      .unsafeToFuture()

object PlaybackMessageHandler:
  private val log = Logger(getClass)
