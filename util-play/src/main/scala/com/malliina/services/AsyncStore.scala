package com.malliina.services

import scala.concurrent.Future

trait AsyncStore[T] {
  def add(item: T): Future[Unit]

  def remove(item: T): Future[Unit]

  def items: Future[Seq[T]]
}
