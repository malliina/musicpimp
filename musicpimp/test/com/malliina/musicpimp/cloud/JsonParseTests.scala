package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.json.PimpStrings
import com.malliina.musicpimp.scheduler.json.{AddApnsDevice, AlarmCommand}
import org.scalatest.FunSuite
import play.api.libs.json.Json

class JsonParseTests extends FunSuite {
  val in =
    """{"cmd":"alarms_edit",
      |"body":{
      |"id":"e0d82212038b938c51dde9f49577ff1f70442fcfe93ec1ff26a2948e36821934",
      |"cmd":"apns_add","tag":"A737BD03-3843-4045-B3D2-FAEACB84A22F"},
      |"request":"c2be3029-fcaa-4e65-9357-017a73b48fcc",
      |"username":"mle"}""".stripMargin
  val json = Json.parse(in)

  test("can parse alarms edit command") {
    val result = (json \ PimpStrings.Body).validate[AlarmCommand]
    assert(result.isSuccess)
    assert(result.get.isInstanceOf[AddApnsDevice])
  }
}
