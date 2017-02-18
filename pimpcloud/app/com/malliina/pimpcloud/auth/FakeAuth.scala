package com.malliina.pimpcloud.auth

import java.util.UUID

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.CloudCredentials
import com.malliina.play.models.Username
import controllers.pimpcloud.{PhoneConnection, ServerRequest}
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class FakeAuth(mat: Materializer) extends CloudAuthentication {
  var server: Option[ServerRequest] = None

  override def authServer(req: RequestHeader): Future[ServerRequest] =
    Future.successful(getOrInit(req))

  override def authPhone(req: RequestHeader): Future[PhoneConnection] =
    Future.successful(PhoneConnection(Username("test"), getOrInit(req).socket))

  override def validate(creds: CloudCredentials): Future[PhoneConnection] =
    Future.failed(new NoSuchElementException)

  def getOrInit(req: RequestHeader) = {
    val s = server.getOrElse(ServerRequest(FakeAuth.FakeUuid, fakeServerSocket(req, mat)))
    server = Option(s)
    s
  }

  def fakeServerSocket(req: RequestHeader, mat: Materializer) = {
    val source = Source.queue[JsValue](100, OverflowStrategy.backpressure)
    val (queue, _) = source.toMat(Sink.asPublisher(fanout = true))(Keep.both).run()(mat)
    new PimpServerSocket(queue, CloudID("test"), req, mat, () => ())
  }
}

object FakeAuth {
  val FakeUuid = UUID.fromString("d3ef33ab-5ba5-4a34-bf7c-9a182c882ab7")
}
