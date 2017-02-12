package com.malliina.musicpimp.models

trait Identifiable {
  def id: String

  override def toString: String = id
}
