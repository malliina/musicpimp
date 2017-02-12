package com.malliina.ws

import com.malliina.maps.StmItemMap
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

class RxStmStorage[C] extends SocketStorage[C] {
  val inner = StmItemMap.empty[C, Unit]
  private val subject = BehaviorSubject[Seq[C]]()
  val users: Observable[Seq[C]] = subject

  override def clients: Seq[C] = inner.keys

  override def onConnect(c: C): Unit = {
    inner.put(c, ())
    subject.onNext(clients)
  }

  override def onDisconnect(c: C): Unit = {
    inner.remove(c)
    subject.onNext(clients)
  }
}

object RxStmStorage {
  def apply[C]() = new RxStmStorage[C]
}
