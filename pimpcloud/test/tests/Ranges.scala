package tests

import com.malliina.play.ContentRange
import com.malliina.storage.StorageInt

class Ranges extends munit.FunSuite {
  test("ContentRange.all") {
    val size = 5.megs
    val asBytes = size.toBytes
    val all = ContentRange.all(size)
    assert(all.endExclusive - all.start == asBytes)
  }
}
