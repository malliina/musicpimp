package com.malliina.musicpimp.audio

import com.malliina.pimpcloud.models.Identifiable

trait MusicItem {
  def id: Identifiable

  def title: String
}
