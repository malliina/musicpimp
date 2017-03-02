package com.malliina.pimpcloud.auth

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.streams.ByteActor
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.InvalidCredentials
import com.malliina.play.models.Username
import controllers.pimpcloud.ServerRequest
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class FakeAuth(mat: Materializer) extends CloudAuthentication {
  var server: Option[ServerRequest] = None

  override def authServer(req: RequestHeader): Future[ServerRequest] =
    Future.successful(getOrInit(req))

  override def authPhone(req: RequestHeader): PhoneAuthResult =
    Future.successful(Right(PhoneConnection(Username("test"), getOrInit(req).socket)))

  override def validate(creds: CloudCredentials): PhoneAuthResult =
    Future.successful(Left(InvalidCredentials(creds.rh)))

  def getOrInit(req: RequestHeader) = {
    val s = server.getOrElse(ServerRequest(FakeAuth.FakeUuid, fakeServerSocket(req, mat)))
    server = Option(s)
    s
  }

  def fakeServerSocket(req: RequestHeader, mat: Materializer) = {
    val source = Source.actorPublisher[ByteString](ByteActor.props())
    val (actor, _) = source.toMat(Sink.asPublisher(fanout = false))(Keep.both).run()(mat)
    new PimpServerSocket(actor, CloudID("test"), req, mat, () => ())
  }
}

object FakeAuth {
  val FakeUuid = RequestID.build("d3ef33ab-5ba5-4a34-bf7c-9a182c882ab7").get
}
