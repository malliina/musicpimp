package tests

import java.nio.file.Paths

import com.malliina.pimpcloud.models.TrackID
import controllers.pimpcloud.Phones
import org.scalatest.FunSuite

class PathTests extends FunSuite {
  test("can encode") {
    val original = "?"
    val encoded = Phones.encode(original)
    val decoded = Phones.decode(encoded)
    assert(decoded === original)
  }

  test("can do apostrophes") {
    val p = TrackID("Dire+Straits%5C%281979%29+Communiqu%C3%A9%5C03+-+Where+Do+You+Think+You%27re+Going..mp3")
    val str = Phones.path(p)
    assert(str.isSuccess)
    val original = "é"
    Paths.get(original)
    assert(1 === 1)
  }
}
