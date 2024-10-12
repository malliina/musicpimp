package tests

import com.malliina.collections.BoundedList

class Collections extends munit.FunSuite {
  test("one-size") {
    val list = BoundedList.empty[Int](1)
    list += 42
    assert(list.size == 1)
    list += 41
    assert(list.size == 1)
  }

  test("buffer") {
    val list = BoundedList.empty[Int](3)
    list += 1
    list += 1
    assert(list.size == 2)
    list += 1
    list += 1
    assert(list.size == 3)
  }
}
