package tests

import com.malliina.audio.javasound.FileJavaSoundPlayer

class ControlTests extends TestBase {

  test("supported controls".ignore) {
    val file = ensureTestMp3Exists()
    val player = new FileJavaSoundPlayer(file)
    assert(player.canAdjustVolume)
  }

  test("volume conversions".ignore) {
    val file = ensureTestMp3Exists()
    val player = new FileJavaSoundPlayer(file)
    val e1 = player.externalVolumeValue(25000, 0, 65663)
    assertEquals(e1, (25000f / 65663f * 100f).toInt)
    val e2 = player.externalVolumeValue(54000, 0, 54000)
    assertEquals(e2, 100)
    val e3 = player.externalVolumeValue(0, 0, 65000)
    assertEquals(e3, 0)
    val i1 = player.internalVolumeValue(50, -100, 100)
    assertEquals(i1, 0f)
    val i2 = player.internalVolumeValue(40, 0, 65536)
    assertEquals(i2.toInt, (0.40 * 65536).toInt)
  }
}
