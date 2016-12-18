package org.musicpimp.js

class Search extends SocketJS("/search/ws?f=json") {
  override def handlePayload(payload: String) = ???
}
