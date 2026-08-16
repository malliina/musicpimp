package com.malliina.pimpcloud.http4s

import cats.effect.Async
import com.malliina.musicpimp.cloud.PimpServerSocket
import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.streams.StreamEndpoint
import com.malliina.play.models.AuthInfo
import com.malliina.values.Username
import org.http4s.Request

case class ServerRequest[F[_]: Async](request: RequestID, socket: PimpServerSocket[F])
  extends AuthInfo:
  override def user: Username = Username.unsafe(socket.id.id)
  override def rh: Request[?] = socket.headers
  def stream: Option[StreamEndpoint[F]] = socket.fileTransfers.find(request)
  def cleanup(success: Boolean): F[Boolean] = socket.fileTransfers.remove(request, false, success)
