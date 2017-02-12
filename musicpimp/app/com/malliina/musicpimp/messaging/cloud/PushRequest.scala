package com.malliina.musicpimp.messaging.cloud

/**
  *
  * @tparam T type of token
  * @tparam M type of message
  */
trait PushRequest[T, M] {
  def tokens: Seq[T]

  def message: M
}
