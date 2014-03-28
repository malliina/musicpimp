package com.mle.play

import play.api.mvc.RequestHeader

/**
 *
 * @author mle
 */
case class RequestInfo(user: String, request: RequestHeader)
