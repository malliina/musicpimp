package com.malliina.musicpimp.messaging

import com.malliina.push.Token

trait TaggedDevice[T <: Token]:
  def id: T

  /** A tag that works as a server ID, used device-side to distinguish between notifications from
    * different MusicPimp servers to the same device.
    *
    * All push notifications will contain this tag.
    *
    * @return
    *   a tag that identifies this server with the device
    */
  def tag: ServerTag
