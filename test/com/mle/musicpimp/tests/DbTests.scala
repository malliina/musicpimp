package com.mle.musicpimp.tests

import org.scalatest.FunSuite
import scala.slick.driver.H2Driver.simple._
import Database.threadLocalSession

/**
 *
 * @author mle
 */
class DbTests extends FunSuite {
  test("in-memory database") {
    object Users extends Table[(Int, String, String)]("users") {
      def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

      def username = column[String]("username")

      def password = column[String]("password")

      def * = id ~ username ~ password

      def forInsert = username ~ password <>((u, p) => (u, p), {
        up: (String, String) => Some(up._1, up._2)
      })
    }

    Database.forURL("jdbc:h2:mem:db/test", driver = classOf[org.h2.Driver].getName) withSession {
      Users.ddl.create
      def users = (for (u <- Users) yield u.username).list
      def deleteUsers() = (for (u <- Users) yield u).delete

      assert(users === List.empty)
      Users.forInsert insert("mle", "pass123")
      assert(users === List("mle"))
      deleteUsers()
      assert(users === List.empty)
    }
  }
}
