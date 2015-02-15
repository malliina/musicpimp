package tests

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.musicpimp.cloud.CloudSocket
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

/**
 * @author Michael
 */
class CloudTests extends FunSuite {
  val uri = "wss://cloud.musicpimp.org/servers/ws2"
  val testUri = "ws://localhost:9000/servers/ws2"
  val testID = "test"
  val failID = "failure"

  //  test("this will block indefinitely"){
  //    val client = new WebSocketClient(URI create testUri) {
  //      override def onError(ex: Exception): Unit = ???
  //
  //      override def onMessage(message: String): Unit = ???
  //
  //      override def onClose(code: Int, reason: String, remote: Boolean): Unit = ???
  //
  //      override def onOpen(handshakedata: ServerHandshake): Unit = ???
  //    }
  //    val isSuccess = client.connectBlocking()
  //    assert(isSuccess)
  //  }
  test("server registration") {
    //    WebSocketImpl.DEBUG = true
    val s = newSocket("incorrect password")
    val fut = connect(s).recoverAll(t => failID)
    val fail = Await.result(fut, 5.seconds)
    assert(fail === failID)
    val socket = newSocket()
    val connFut = connect(socket)
    val id = Await.result(connFut, 5.seconds)
    assert(id === testID)
    socket.unregister()
    socket.close()
    val socket2 = newSocket()
    val connFut2 = connect(socket2)
    val id2 = Await.result(connFut2, 5.seconds)
    assert(id2 === testID)
    val socket3 = newSocket()
    val connFut3 = connect(socket3)
    val id3 = Await.result(connFut3, 5.seconds)
    assert(id3 === failID)
    socket2.close()
    socket3.close()
  }

  def newSocket(pass: String = "pimp") = new CloudSocket(testUri, testID, pass)

  def connect(socket: CloudSocket) = socket.connect().flatMap(_ => socket.registration).recoverAll(t => failID)
}
