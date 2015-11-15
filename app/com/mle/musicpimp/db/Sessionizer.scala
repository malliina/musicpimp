package com.mle.musicpimp.db

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple.Session

/**
  * @author mle
  */
class Sessionizer(db: PimpDb) {
  protected def withSession[T](body: Session => T): Future[T] = db.withSession(body)
}
