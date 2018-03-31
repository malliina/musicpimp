package tests

import com.malliina.audio.AudioImplicits
import org.scalatest.FunSuite

class Tests extends FunSuite {
  test("tests work") {}

  test("time formatting") {
    import AudioImplicits._

    import concurrent.duration._
    def assertSeconds(secs: Int, expected: String) =
      assert(secs.seconds.readable === expected)
    assertSeconds(0, "00:00")
    assertSeconds(5, "00:05")
    assertSeconds(60, "01:00")
    assertSeconds(100, "01:40")
    assertSeconds(1000, "16:40")
    assertSeconds(3600, "01:00:00")
    assertSeconds(10000, "02:46:40")
  }
}
