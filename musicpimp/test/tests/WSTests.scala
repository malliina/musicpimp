package tests

import com.malliina.musicpimp.beam.BeamCommand
import org.scalatest.FunSuite
import play.api.libs.json._

class WSTests extends FunSuite {
  test("json to case class") {
    val json = Json.obj(
      "action" -> "play",
      "track" -> "123",
      "uri" -> "http://www.musicpimp.org",
      "username" -> "hm",
      "password" -> "secret"
    )
    implicit val commandFormat = Json.format[BeamCommand]
    val cmd = Json.fromJson[BeamCommand](json).get
    assert(cmd.track.id === "123")
  }
}
