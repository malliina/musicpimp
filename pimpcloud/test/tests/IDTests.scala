package tests

import java.nio.charset.StandardCharsets
import java.util.UUID

import org.apache.commons.codec.binary.Base64
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

  test("base64 encode") {
    val enc = Base64.encodeBase64String(s"a:b:c".getBytes(StandardCharsets.UTF_8))
    println(enc)
  }
}
