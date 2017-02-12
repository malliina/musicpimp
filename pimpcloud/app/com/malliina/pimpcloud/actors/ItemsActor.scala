package com.malliina.pimpcloud.actors

import akka.actor.Actor

/** Manages the state of a data structure.
  *
  * @tparam T type of item
  */
trait ItemsActor[T] extends Actor {
  var clients: Set[T] = Set.empty[T]
}
