package com.malliina.musicpimp.messaging

import com.malliina.push.Token

/**
  * @author Michael
  */
trait AndroidDevice[T <: Token] {
  def id: T

  def tag: String
}
