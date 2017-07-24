package com.malliina.musicpimp.db

import slick.dbio.{DBIOAction, NoStream}
import slick.lifted.Query

import scala.concurrent.Future
import scala.language.higherKinds

class Sessionizer(db: PimpDb) {
  def runQuery[A, B, C[_]](query: Query[A, B, C]): Future[C[B]] = db.runQuery(query)

  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = db.run(a)
}
