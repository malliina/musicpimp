package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.PlaybackMessageHandler.log
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, RemoteInfo, TrackID}
import play.api.Logger
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PlaybackMessageHandler(lib: MusicLibrary, statsPlayer: StatsPlayer)(implicit ec: ExecutionContext)
  extends JsonHandlerBase {

  val player = MusicPlayer
  val playlist = MusicPlayer.playlist

  override def handleMessage(msg: JsValue, src: RemoteInfo): Unit = {
    statsPlayer.updateUser(src.user)
    super.handleMessage(msg, src)
  }

  override protected def fulfillMessage(message: PlayerMessage, src: RemoteInfo): Unit = {
    message match {
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
        player.volume(vol)
      case SeekMsg(pos) =>
        player.seek(pos)
      case PlayMsg(track) =>
        player.reset(Library meta track)
      case SkipMsg(index) =>
        Try(player skip index).recover {
          case iae: IllegalArgumentException =>
            log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
        }
      case AddMsg(track) =>
        playlist.add(Library meta track)
      case InsertTrackMsg(index, track) =>
        playlist.insert(index, Library meta track)
      case MoveTrackMsg(from, to) =>
        playlist.move(from, to)
      case RemoveMsg(index) =>
        playlist.delete(index)
      case AddAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks).map(_.foreach(playlist.add))
      case PlayAllMsg(tracks, folders) =>
        resolveTracksOrEmpty(folders, tracks) map {
          case head :: tail =>
            player.reset(head)
            tail.foreach(playlist.add)
          case Nil =>
            log warn s"No tracks were resolved"
        }
      case ResetPlaylistMessage(index, tracks) =>
        playlist.reset(index, tracks.map(Library.meta))
      case _ =>
        log error s"Unsupported message: $message"
    }
  }

  def resolveTracksOrEmpty(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[Seq[LocalTrack]] =
    resolveTracks(folders, tracks) recover {
      case t: Throwable =>
        log error s"Unable to resolve tracks from ${folders.size} folder and ${tracks.size} track references"
        Nil
    }

  def resolveTracks(folders: Seq[FolderID], tracks: Seq[TrackID]): Future[Seq[LocalTrack]] = {
    Future.traverse(folders)(folder => lib.tracksIn(folder).map(_.getOrElse(Nil)).map(Library.localize))
      .map(_.flatten).map(subTracks => tracks.map(Library.meta) ++ subTracks)
  }
}

object PlaybackMessageHandler {
  private val log = Logger(getClass)
}
