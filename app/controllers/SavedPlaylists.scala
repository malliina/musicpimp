package controllers

import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.library.PlaylistService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * @author mle
 */
class SavedPlaylists(service: PlaylistService) extends Secured {
  def playlist(id: String) = pimpActionAsync2(auth => service.playlist(id))

  def playlists = pimpActionAsync2(auth => service.playlists)

  def add(playlist: String) = pimpParsedActionAsync2(parse.json)(auth => {
    // val payload = auth.body
    // TODO parse payload and handle it
    Future.successful(JsonMessages.thanks)
  })

  def remove(id: String) = pimpActionAsync2(auth => service.remove(id).map(_ => JsonMessages.thanks))
}
