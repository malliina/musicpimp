package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.PlaybackMessageHandler.log
import com.malliina.musicpimp.json.{JsonFormatVersions, JsonMessages}
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, RemoteInfo, TrackID}
import com.malliina.values.Username
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PlaybackMessageHandler(lib: MusicLibrary, statsPlayer: StatsPlayer)(implicit ec: ExecutionContext)
  extends JsonHandlerBase {

  val player = MusicPlayer
  val playlist = MusicPlayer.playlist

  def updateUser(user: Username): Unit = statsPlayer.updateUser(user)

  override def handleMessage(msg: JsValue, src: RemoteInfo): Unit = {
    statsPlayer.updateUser(src.user)
    super.handleMessage(msg, src)
  }

  override def fulfillMessage(message: PlayerMessage, src: RemoteInfo): Unit = {
    message match {
      case GetStatusMsg =>
        val json = src.apiVersion match {
          case JsonFormatVersions.JSONv17 => toJson(player.status17(src.host))
          case _ => toJson(player.status(src.host))
        }
        src.target send JsonMessages.withStatus(json)
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
        Try(player skip index).recover {
          case iae: IllegalArgumentException =>
            log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
        }
      case AddMsg(track) =>
        withTrack(track)(playlist.add)
      case InsertTrackMsg(index, track) =>
        withTrack(track) { t => playlist.insert(index, t) }
      case MoveTrackMsg(from, to) =>
        playlist.move(from, to)
      case RemoveMsg(index) =>
        playlist.delete(index)
      case AddAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks).map(_.foreach(playlist.add))
      case PlayAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks).map {
          case head :: tail =>
            player.reset(head)
            tail.foreach(playlist.add)
          case Nil =>
            log warn s"No tracks were resolved"
        }
      case ResetPlaylistMessage(index, tracks) =>
        lib.tracks(tracks).map { ts =>
          playlist.reset(index, ts)
        }.recover {
          case e: Exception =>
            log.error("Unable to reset playlist.", e)
        }
      case Handover(index, tracks, state, position) =>
        lib.tracks(tracks).map { ts =>
          playlist.reset(index.getOrElse(BasePlaylist.NoPosition), ts)
          playlist.current.foreach { t =>
            for {
              _ <- player.tryInitTrackWithFallback(t)
              _ <- player.trySeek(position)
            } yield {
              if (PlayState.isPlaying(state)) {
                player.play()
              }
            }
          }
        }.recover {
          case e: Exception =>
            log.error("Handover failed.", e)
        }
      case other =>
        log.warn(s"Unsupported message: '$other'.")
    }
  }

  def withTrack(id: TrackID)(code: LocalTrack => Unit) =
    lib.meta(id).map { t =>
      t.map { local =>
        code(local)
      }.getOrElse {
        log.error(s"Track not found: '$id'.")
      }
    }.recover {
      case e: Exception =>
        log.error(s"Track search failed.", e)
    }

  def resolveTracksOrEmpty(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[List[LocalTrack]] =
    resolveTracks(folders, tracks).map(_.toList).recover {
      case t: Exception =>
        log.error(s"Unable to resolve tracks from ${folders.size} folder and ${tracks.size} track references", t)
        Nil
    }

  def resolveTracks(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[Seq[LocalTrack]] = {
    Future.traverse(folders)(folder => lib.tracksIn(folder).map(_.getOrElse(Nil)).map(Library.localize))
      .map(_.flatten).flatMap(subTracks => lib.tracks(tracks).map(lts => lts ++ subTracks))
  }
}

object PlaybackMessageHandler {
  private val log = Logger(getClass)
}
