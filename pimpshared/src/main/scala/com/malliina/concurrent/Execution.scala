package com.malliina.concurrent

import cats.effect.unsafe.IORuntime

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Execution:
  implicit val cached: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  implicit val runtime: IORuntime = cats.effect.unsafe.implicits.global
