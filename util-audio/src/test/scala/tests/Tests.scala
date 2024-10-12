package tests

import com.malliina.audio.AudioImplicits
import munit.FunSuite

class Tests extends FunSuite:
  test("tests work") {}

  test("time formatting"):
    import AudioImplicits.*

    import concurrent.duration.*
    def assertSeconds(secs: Int, expected: String) =
      assertEquals(secs.seconds.readable, expected)
    assertSeconds(0, "00:00")
    assertSeconds(5, "00:05")
    assertSeconds(60, "01:00")
    assertSeconds(100, "01:40")
    assertSeconds(1000, "16:40")
    assertSeconds(3600, "01:00:00")
    assertSeconds(10000, "02:46:40")
