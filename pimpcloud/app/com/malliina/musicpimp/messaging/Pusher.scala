package com.malliina.musicpimp.messaging

import scala.concurrent.Future

trait Pusher {
  def push(pushTask: PushTask): Future[PushResult]
}
