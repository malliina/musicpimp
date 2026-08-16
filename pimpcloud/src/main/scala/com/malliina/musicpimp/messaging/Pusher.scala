package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}

import scala.concurrent.Future

trait Pusher[F[_]]:
  def push(pushTask: PushTask): F[PushResult]
