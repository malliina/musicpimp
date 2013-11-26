package com.mle.musicpimp.db

import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

/**
 * Unused.
 *
 * @author mle
 */
trait PimpDB {

  object Users extends Table[(Int, String, String)]("users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def username = column[String]("username")

    def password = column[String]("password")

    def * = id ~ username ~ password
  }

  Database.forURL("jdbc:h2:mem:db/test", driver = classOf[org.h2.Driver].getName) withSession {
    (for (u <- Users) yield u.username).list
  }


}

object PimpDB extends PimpDB
