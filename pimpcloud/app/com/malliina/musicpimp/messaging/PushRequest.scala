package com.malliina.musicpimp.messaging

/**
  *
  * @tparam T type of token
  * @tparam M type of message
  */
trait PushRequest[T, M] {
  def tokens: Seq[T]

  def message: M
}
