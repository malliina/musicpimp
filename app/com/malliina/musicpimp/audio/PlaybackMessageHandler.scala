package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.PlaybackMessageHandler.log
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models.User
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.util.Try

class PlaybackMessageHandler(lib: MusicLibrary, statsPlayer: StatsPlayer)
  extends JsonHandlerBase {

  val player = MusicPlayer
  val playlist = MusicPlayer.playlist

  def handleMessage(msg: JsValue): Unit = handleMessage(msg, "")

  override protected def handleMessage(msg: JsValue, user: String): Unit = {
    statsPlayer.updateUser(User(user))
    super.handleMessage(msg, user)
  }

  override def fulfillMessage(message: PlayerMessage, user: String): Unit = {
    message match {
      case Resume =>
        player.play()
      case Stop =>
        player.stop()
      case Next =>
        player.nextTrack()
      case Prev =>
        player.previousTrack()
      case Mute(isMute) =>
        player.mute(isMute)
      case Volume(vol) =>
        player.volume(vol)
      case Seek(pos) =>
        player.seek(pos)
      case Play(track) =>
        player.reset(Library meta track)
      case Skip(index) =>
        Try(player skip index).recover {
          case iae: IllegalArgumentException =>
            log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
        }
      case Add(track) =>
        playlist.add(Library meta track)
      case InsertTrack(index, track) =>
        playlist.insert(index, Library meta track)
      case MoveTrack(from, to) =>
        playlist.move(from, to)
      case Remove(index) =>
        playlist.delete(index)
      case AddAll(folders, tracks) =>
        resolveTracksOrEmpty(folders, tracks).map(_.foreach(playlist.add))
      case PlayAll(folders, tracks) =>
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

  def resolveTracksOrEmpty(folders: Seq[String], tracks: Seq[String]): Future[Seq[LocalTrack]] =
    resolveTracks(folders, tracks).recover {
      case t: Throwable =>
        log error s"Unable to resolve tracks from ${folders.size} folder and ${tracks.size} track references"
        Nil
    }

  def resolveTracks(folders: Seq[String], tracks: Seq[String]): Future[Seq[LocalTrack]] = {
    Future.traverse(folders)(folder => lib.tracksIn(folder).map(_.getOrElse(Nil)).map(Library.localize))
      .map(_.flatten).map(subTracks => tracks.map(Library.meta) ++ subTracks)
  }
}

object PlaybackMessageHandler {
  private val log = Logger(getClass)
}
