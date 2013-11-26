package com.mle.musicpimp.tests

import org.scalatest.FunSuite
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.Await
import play.api.mvc.Controller
import java.net.{URI, URLEncoder}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import java.nio.file.Paths
import com.mle.http.MultipartRequest
import play.api.libs.json._
import concurrent.duration._
import play.api.libs.json.JsSuccess
import com.mle.musicpimp.beam.BeamCommand
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import collection.JavaConversions._


/**
 * @author Michael
 */
class WSTests extends FunSuite with Controller {

  case class AlbumCover(uri: URI, size: String)

  private def encode(input: String) = URLEncoder.encode(input, "UTF-8")

  test("discogs") {
    val artist = "Iron Maiden"
    val album = "Powerslave"
    val artistEnc = encode(artist)
    val albumEnc = encode(album)
    // 251595
    val uri = s"http://api.discogs.com/database/search?artist=$artistEnc&release_title=$albumEnc"
    val responseFuture = WS.url(uri).get().map(_.json)
    val response = Await.result(responseFuture, 5 seconds)
    println(Json.stringify(response))
    val thumbUri = (response \ "results" \\ "thumb").headOption
    thumbUri.map(t => println(Json.stringify(t)))
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
