package com.malliina.musicpimp.messaging

import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}

import scala.concurrent.Future

trait Pusher {
  def push(pushTask: PushTask): Future[PushResult]
}
