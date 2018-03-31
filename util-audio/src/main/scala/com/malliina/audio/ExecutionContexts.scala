package com.malliina.audio

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object ExecutionContexts {
  implicit val defaultPlaybackContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  implicit val singleThreadContext: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
}
