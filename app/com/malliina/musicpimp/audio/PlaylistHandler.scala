package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.PlaylistID
import play.api.libs.json._

/**
 * @author mle
 */
class PlaylistHandler(service: PlaylistService) {
  val Id = "id"

  trait Command

  case object GetPlaylists extends Command

  case class GetPlaylist(id: PlaylistID) extends Command

  case class SavePlaylist(playlist: PlaylistSubmission) extends Command

  case class DeletePlaylist(id: PlaylistID) extends Command

  def parseCommand(json: JsValue): JsResult[Command] = {
    (json \ CMD).validate[String].flatMap {
      case PlaylistsGet =>
        JsSuccess(GetPlaylists)
      case PlaylistGet =>
        (json \ Id).validate[PlaylistID].map(GetPlaylist)
      case PlaylistSave =>
        (json \ PlaylistKey).validate[PlaylistSubmission].map(SavePlaylist)
      case PlaylistDelete =>
        (json \ Id).validate[PlaylistID].map(DeletePlaylist)
      case other =>
        JsError(s"Unknown command: $other")
    }
  }
}
