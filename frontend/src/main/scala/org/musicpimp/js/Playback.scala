package org.musicpimp.js

class Playback extends SocketJS("/ws/playback") {
  override def handlePayload(payload: String) = println("Hello, world!")
}
