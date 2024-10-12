package com.malliina.html

class BootstrapTests extends munit.FunSuite {
  test("bootstrap helpers") {
    object bs extends BootstrapParts
    val col = bs.col
    assert(col.six == "col-6")
    assert(col.md.six == "col-md-6")
    assert(col.md.offset.four == "offset-md-4")
    assert(col.lg.width("1") == "col-lg-1")
  }
}
