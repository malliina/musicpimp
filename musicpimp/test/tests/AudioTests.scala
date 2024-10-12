package tests

import javax.sound.sampled.AudioSystem

import com.malliina.util.Util

class AudioTests extends munit.FunSuite:
  test("has audio device".ignore):
    val status = AudioSystem.getMixerInfo.map(i =>
      s"Name: ${i.getName}, Description: ${i.getDescription}, Version: ${i.getVersion}"
    )
    //    status foreach println
    assert(status.length > 0)

  test("can find resource"):
    val resourceURL = Util.resourceOpt("mpthreetest.mp3")
    assert(resourceURL.isDefined)
