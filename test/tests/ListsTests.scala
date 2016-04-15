package tests

import com.malliina.util.Lists
import org.scalatest.FunSuite

class ListsTests extends FunSuite {
  test("Lists.move respects postconditions") {
    val before = Seq(1, 2, 3, 4, 5, 6)
    val from = 2
    val to = 4
    val expected = Seq(1, 2, 4, 5, 3, 6)
    assert(Lists.move(from, to, before) === expected)
  }
}
