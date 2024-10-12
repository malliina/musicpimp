package com.malliina.play

import play.api.http.HeaderNames.USER_AGENT
import play.api.mvc.RequestHeader

package object http {
  implicit class RequestHeaderOps(rh: RequestHeader) {
    def realAddress = Proxies.realAddress(rh)
    def userAgent = rh.headers.get(USER_AGENT)
    def describe = RequestHeaderOps.describe(rh)
  }

  object RequestHeaderOps {
    def describe(req: RequestHeader): String = {
      val userAgent = req.userAgent getOrElse "undefined"
      s"${req.method} '${req.uri}' from '${Proxies.realAddress(req)}' using '$userAgent'"
    }
  }
}
