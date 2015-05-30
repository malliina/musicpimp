package controllers

import java.net.InetSocketAddress

import com.mle.musicpimp.Starter
import com.mle.util.{Log, Utils}
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.Channel
import play.api.mvc.RequestHeader
import play.core.server.NettyServer

object RequestHelpers extends Log {
  def httpPort = Starter.nettyServer.flatMap(_.httpPort)

  def httpsPort = Starter.nettyServer.flatMap(_.httpsPort)

  def port(req: RequestHeader): Int = (if (req.secure) httpsPort else httpPort) orElse portFromHost(req) getOrElse 80

  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.length > 1) Utils.opt[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }
}
