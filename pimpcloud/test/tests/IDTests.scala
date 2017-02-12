package tests

import java.util.UUID

import org.scalatest.FunSuite

import scala.collection.concurrent.TrieMap

class IDTests extends FunSuite {
  val map = TrieMap.empty[UUID, Int]

  test("UUID.equals") {
    val uuid = UUID.randomUUID()
    val uuidString = uuid.toString
    val uuid2 = UUID.fromString(uuidString)
    assert(uuid === uuid2)
    map += (uuid -> 1)
    val i = map get uuid2
    assert(i contains 1)
  }
}
