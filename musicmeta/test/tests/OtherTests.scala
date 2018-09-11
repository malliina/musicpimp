package tests

import java.nio.file.Paths

import org.scalatest.FunSuite

class OtherTests extends FunSuite {
  test("read a path") {
    val p = Paths.get("é")
    val f = p.toFile
    assert(p.toAbsolutePath.toString === f.getAbsolutePath)
  }
}
