package tests

import java.nio.file.Paths

class OtherTests extends munit.FunSuite {
  test("read a path") {
    val p = Paths.get("é")
    val f = p.toFile
    assert(p.toAbsolutePath.toString == f.getAbsolutePath)
  }
}
