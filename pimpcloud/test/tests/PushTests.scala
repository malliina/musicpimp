package tests

import com.malliina.musicpimp.messaging.{APNSRequest, ProdPusher, PushTask}
import com.malliina.push.apns.{APNSMessage, APNSToken}
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PushTests extends FunSuite {
  val testTask = PushTask(
    Option(
      APNSRequest(Seq(APNSToken.build("9f3c2f830256954ada78bf56894fa7586307f0eedb7763117c84e0c1eee8347a").get),
        APNSMessage.badged("Simple message", badge = 7))),
    None,
    None,
    None,
    None
  )

  ignore("can push") {
    val pusher = ProdPusher.fromConf
    val res = Await.result(pusher.push(testTask), 30.seconds)
    assert(res.apns.size === 1)
  }
}
