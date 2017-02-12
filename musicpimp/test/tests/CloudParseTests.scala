package tests

import com.malliina.musicpimp.cloud.CloudMessageParser
import com.malliina.musicpimp.cloud.PimpMessages.GetRecent
import org.scalatest.FunSuite
import play.api.libs.json.Json

class CloudParseTests extends FunSuite {
  val testJsonString =
    """
      |{
      |  "cmd": "recent",
      |  "request": "test request id",
      |  "username": "test user",
      |  "body": {
      |    "from": 13,
      |    "until": 20
      |  }
      |}
    """.stripMargin

  val testJson = Json.parse(testJsonString)

  test("can parse a most recent request payload") {
    val result = CloudMessageParser.parseRequest(testJson)
    assert(result.isSuccess)
    val (message, _) = result.get
    assert(message.isInstanceOf[GetRecent])
    val meta = message.asInstanceOf[GetRecent].meta
    assert(meta.from === 13)
    assert(meta.until === 20)
  }
}
