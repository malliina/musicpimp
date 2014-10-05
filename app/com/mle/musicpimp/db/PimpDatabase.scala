package com.mle.musicpimp.db

import com.mle.db.DatabaseLike

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
trait PimpDatabase extends DatabaseLike {
  val tracks = TableQuery[Tracks]
  val folders = TableQuery[Folders]
  val tokens = TableQuery[TokensTable]
}
