package com.malliina.play.controllers

import com.malliina.web.HttpConstants
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.http.Writeable
import play.api.mvc.{Result, Results}

object Caching extends Caching

trait Caching {
  def NoCacheOk[C: Writeable](content: C): Result =
    NoCache(Results.Ok(content))

  def NoCache[T](result: => Result): Result =
    result.withHeaders(CACHE_CONTROL -> HttpConstants.NoCache)
}
