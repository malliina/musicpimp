package com.malliina.play.models

trait Identifiable {
  def id: String

  override def toString: String = id
}
