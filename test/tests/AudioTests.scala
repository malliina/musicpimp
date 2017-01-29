package tests

import javax.sound.sampled.AudioSystem

import org.scalatest.FunSuite

class AudioTests extends FunSuite {
  ignore("has audio device") {
    val status = AudioSystem.getMixerInfo.map(i => s"Name: ${i.getName}, Description: ${i.getDescription}, Version: ${i.getVersion}")
    //    status foreach println
    assert(status.length > 0)
  }
}
