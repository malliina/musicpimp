package controllers

import com.malliina.musicpimp.Starter
import com.malliina.util.Utils
import play.api.mvc.RequestHeader

object RequestHelpers {
  def httpPort = Starter.nettyServer.flatMap(_.httpPort)

  def httpsPort = Starter.nettyServer.flatMap(_.httpsPort)

  def port(req: RequestHeader): Int = (if (req.secure) httpsPort else httpPort) orElse portFromHost(req) getOrElse 80

  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.length > 1) Utils.opt[Int, NumberFormatException](maybeSuffix.tail.toInt)
    else None
  }
}
