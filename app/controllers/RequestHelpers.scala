package controllers

import play.api.mvc.RequestHeader
import com.mle.musicpimp.Starter
import java.net.InetSocketAddress
import play.core.server.NettyServer
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import com.mle.util.{Util, Log}

object RequestHelpers extends Log {
  /**
   * May not work if the HTTPS port is 80 and excluded from the request's <code>host</code> member.
   *
   * @param request
   * @return true if the request was made over HTTPS, false otherwise
   */
  def isHttps(request: RequestHeader): Boolean =
    httpsPort.exists(sslPort => request.host.contains(s":$sslPort"))

  def httpsPort = port(_.HTTPS)

  def httpPort = port(_.HTTP)

  def port(req: RequestHeader): Int = (if (isHttps(req)) httpsPort else httpPort) orElse portFromHost(req) getOrElse 80

  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.size > 1) Util.optionally[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }

  private def port(f: NettyServer => Option[(ServerBootstrap, Channel)]): Option[Int] =
    Starter.nettyServer.flatMap(server => f(server)
      .flatMap(pair => Option(pair._2.getLocalAddress)
      .map(_.asInstanceOf[InetSocketAddress].getPort)))
}

