package com.malliina.concurrent

import scala.concurrent.ExecutionContext

object Execution {
  implicit val cached: ExecutionContext = com.malliina.play.auth.Execution.cached
}
