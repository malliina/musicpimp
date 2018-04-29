package tests

import com.malliina.musicpimp.messaging.cloud.PushResponse
import com.malliina.musicpimp.models.RequestID
import org.scalatest.FunSuite
import play.api.libs.json.Json

class ModelTests extends FunSuite {
  test("RequestID validation") {
    val bogus = RequestID.build("")
    assert(bogus.isEmpty)
  }

  test("model validation and equality") {
    // Does not compile
    //    RequestID("123")
    val id1 = RequestID.build("123").get
    val id2 = RequestID.build("123").get
    assert(id1 === id2)

  }

  test("parse push") {
    val in = """{"result":{"apns":[{"token":"193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9","error":"UnknownReason"},{"token":"e0d82212038b938c51dde9f49577ff1f70442fcfe93ec1ff26a2948e36821934","id":"6972B9B7-9461-5F95-F82E-2508ECB47D2C"},{"token":"193942675140b3d429311de140bd08ff423712ec9c3ea365b12e61b84609afa9","error":"UnknownReason"},{"token":"e0d82212038b938c51dde9f49577ff1f70442fcfe93ec1ff26a2948e36821934","error":"BadDeviceToken"}],"gcm":[{"ids":["APA91bHR4yng5EXmeQZvl8MQB-t9_R33-0ScxK4U10Jtc9QPBg34s1biuA_sBJdOz6VsYcWDldPq8yUePbJV9k0TesQZCf1lgnQmgi7OohpBmlokw5TQ6OxBOKvtMBA5GSzr_QDQm7DCloQ5AnsW8gcPR_GUr_tcDg"],"response":{"multicast_id":8653192098838126408,"success":1,"failure":0,"canonical_ids":0,"results":[{"message_id":"0:1525024776190915%bc6f8a77f9fd7ecd"}]}},{"ids":["APA91bGrvnDeCTk7neV4yjN5CPtbMF7XuSpsxgA4B4K9knDsBgzPn8PaVuz1o_50ot2-ydNZJ8smTLehT6hehmbXtVi-s4kBJkkavXohgWgrbHo6vVtbPP4"],"response":{"multicast_id":5135888163875590668,"success":1,"failure":0,"canonical_ids":0,"results":[{"message_id":"0:1525024776206396%bc6f8a77f9fd7ecd"}]}}],"adm":[],"mpns":[],"wns":[]}}"""
    val result = Json.parse(in).validate[PushResponse]
//    println(result)
    assert(result.isSuccess)
  }
}
