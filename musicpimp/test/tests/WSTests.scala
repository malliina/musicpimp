package tests

import com.malliina.musicpimp.beam.BeamCommand
import play.api.libs.json._

class WSTests extends munit.FunSuite {
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
    assert(cmd.track.id == "123")
  }
}
