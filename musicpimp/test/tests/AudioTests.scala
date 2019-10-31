package tests

import javax.sound.sampled.AudioSystem

import com.malliina.util.Util
import org.scalatest.FunSuite

class AudioTests extends FunSuite {
  ignore("has audio device") {
    val status = AudioSystem.getMixerInfo.map(
      i => s"Name: ${i.getName}, Description: ${i.getDescription}, Version: ${i.getVersion}"
    )
    //    status foreach println
    assert(status.length > 0)
  }

  test("can find resource") {
    val resourceURL = Util.resourceOpt("mpthreetest.mp3")
    assert(resourceURL.isDefined)
  }
}
