package tests

import com.malliina.musicpimp.beam.BeamCommand
import io.circe.Json
import io.circe.syntax.EncoderOps

class WSTests extends munit.FunSuite:
  test("json to case class"):
    val json = Json.obj(
      "action" -> "play".asJson,
      "track" -> "123".asJson,
      "uri" -> "http://www.musicpimp.org".asJson,
      "username" -> "hm".asJson,
      "password" -> "secret".asJson
    )
    val cmd = json.as[BeamCommand].toOption.get
    assert(cmd.track.id == "123")
