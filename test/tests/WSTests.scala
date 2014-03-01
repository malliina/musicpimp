package tests

import org.scalatest.FunSuite
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.Await
import play.api.mvc.Controller
import java.net.URLEncoder
import play.api.libs.json._
import concurrent.duration._
import com.mle.musicpimp.beam.BeamCommand

/**
 * @author Michael
 */
class WSTests extends FunSuite with Controller {
  private def encode(input: String) = URLEncoder.encode(input, "UTF-8")

  test("can download album cover from discogs") {
    val artist = "Iron Maiden"
    val album = "Powerslave"
    val artistEnc = encode(artist)
    val albumEnc = encode(album)
    // 251595
    val uri = s"http://api.discogs.com/database/search?artist=$artistEnc&release_title=$albumEnc"
    // http://api.discogs.com/image/R-90-5245462-1388609959-3809.jpeg
    val operation =
      WS.url(uri).get().map(_.json)
        .map(json => (json \ "results" \\ "thumb").head).map(Json.stringify)
        .flatMap(str => WS.url(str.tail.init).get())
    val response = Await.result(operation, 5 seconds)
    assert(response.status === 200)
  }

  test("json to case class") {
    val json = Json.obj(
      "action" -> "play",
      "track" -> "123",
      "uri" -> "http...",
      "username" -> "hm",
      "password" -> "secret"
    )
    implicit val commandFormat = Json.format[BeamCommand]
    val cmd = Json.fromJson[BeamCommand](json).get
    assert(cmd.track === "123")
  }
}
