package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicLibrary}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.util.Try

/**
  *
  * @author mle
  */
class PlaybackMessageHandler(lib: MusicLibrary) extends JsonHandlerBase {
  def handleMessage(msg: JsValue): Unit = handleMessage(msg, "")

  override protected def handleMessage(msg: JsValue, user: String): Unit = {
    withCmd(msg)(cmd => cmd.command match {
      case RESUME =>
        MusicPlayer.play()
      case STOP =>
        MusicPlayer.stop()
      case NEXT =>
        MusicPlayer.nextTrack()
      case PREV =>
        MusicPlayer.previousTrack()
      case MUTE =>
        MusicPlayer.mute(cmd.boolValue)
      case VOLUME =>
        val vol = cmd.value
        MusicPlayer.volume(vol)
      case SEEK =>
        val pos = cmd.value
        MusicPlayer.seek(pos.toDouble.seconds)
      case PLAY =>
        val track = cmd.track
        MusicPlayer.reset(Library meta track)
      case SKIP =>
        val index = cmd.indexOrValue
        Try(MusicPlayer skip index).recover {
          case iae: IllegalArgumentException => log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
        }
      case ADD =>
        val track = cmd.track
        MusicPlayer.playlist.add(Library meta track)
      case Insert =>
        val track = cmd.track
        MusicPlayer.playlist.insert(cmd.indexOrValue, Library meta track)
      case Move =>
        for {
          from <- (msg \ From).asOpt[Int]
          to <- (msg \ To).asOpt[Int]
        } yield MusicPlayer.playlist.move(from, to)
      case REMOVE =>
        MusicPlayer.playlist.delete(cmd.indexOrValue)
      case ADD_ITEMS =>
        resolveTracks(cmd).map(ts => {
          ts.foreach(MusicPlayer.playlist.add)
        })
      case PLAY_ITEMS =>
        resolveTracks(cmd).map(ts => {
          if (ts.nonEmpty) {
            MusicPlayer.reset(ts.head)
            ts.tail.foreach(MusicPlayer.playlist.add)
          }
        })
      case ResetPlaylist =>
        val index = cmd.indexOpt getOrElse BasePlaylist.NoPosition
        val tracks = cmd.tracksOrNil.map(Library.meta)
        MusicPlayer.playlist.reset(index, tracks)
      case anythingElse =>
        log error s"Invalid JSON: $msg"
    })

    def resolveTracks(cmd: JsonCmd): Future[Seq[LocalTrack]] = {
      val folders = cmd.foldersOrNil
      val tracks = cmd.tracksOrNil

      Future.traverse(folders)(folder => lib.tracksIn(folder).map(_.getOrElse(Nil)).map(Library.localize))
        .map(_.flatten).map(subTracks => tracks.map(Library.meta) ++ subTracks)
    }
  }
}
