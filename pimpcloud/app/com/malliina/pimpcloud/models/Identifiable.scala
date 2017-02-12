package com.malliina.pimpcloud.models

trait Identifiable {
  def id: String

  override def toString: String = id
}
