package com.malliina.pimpcloud.actors

/** Base operations for messages and clients.
  *
  * @tparam M type of message
  * @tparam C type of client
  */
trait MessagesBase[M, C] {

  case class Connect(client: C)

  case class Disconnect(client: C)

  case class Message(message: M, recipient: C)

  case class Broadcast(message: M)

  case object GetClients

  case class Clients(clients: Set[C])

}
