package tests

import com.malliina.musicpimp.cloud.{CloudMessageParser, GetRecent}

class CloudParseTests extends munit.FunSuite:
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

  val testJson = io.circe.parser.parse(testJsonString).toOption.get

  test("can parse a most recent request payload"):
    val result = CloudMessageParser.parseRequest(testJson)
    assert(result.isRight)
    val message = result.toOption.get.message
    assert(message.isInstanceOf[GetRecent])
    val meta = message.asInstanceOf[GetRecent].meta
    assert(meta.from == 13)
    assert(meta.until == 20)
