package tests

import com.malliina.musicpimp.messaging.cloud.{APNSPayload, PushTask}
import com.malliina.push.apns.{APNSMessage, APNSToken}
import org.scalatest.FunSuite

class PushTests extends FunSuite {
  val testTask = PushTask(
    Seq(
      APNSPayload(APNSToken.build("9f3c2f830256954ada78bf56894fa7586307f0eedb7763117c84e0c1eee8347a").get,
        APNSMessage.badged("Simple message", badge = 7))),
    Nil,
    Nil,
    Nil,
    Nil
  )

  ignore("can push") {
    //    val pusher = ProdPusher.fromConf
    //    val res = Await.result(pusher.push(testTask), 30.seconds)
    //    assert(res.apns.size === 1)
  }
}
