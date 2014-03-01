package tests

import org.scalatest.FunSuite
import javax.sound.sampled.AudioSystem
import collection.JavaConversions._

/**
 *
 * @author mle
 */
class AudioTests extends FunSuite {
  test("audio devices") {
    val status = AudioSystem.getMixerInfo.map(i => s"Name: ${i.getName}, Description: ${i.getDescription}, Version: ${i.getVersion}")
    status foreach println
  }
}
