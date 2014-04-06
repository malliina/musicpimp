package tests

import org.scalatest.FunSuite
import javax.sound.sampled.AudioSystem

/**
 *
 * @author mle
 */
class AudioTests extends FunSuite {
  test("has audio device") {
    val status = AudioSystem.getMixerInfo.map(i => s"Name: ${i.getName}, Description: ${i.getDescription}, Version: ${i.getVersion}")
    //    status foreach println
    assert(status.size > 0)
  }
}
