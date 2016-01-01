package tests

import java.io.FileInputStream
import java.nio.file.{Files, Paths}

import com.malliina.musicpimp.http.RangedInputStream
import com.malliina.play.ContentRange
import com.malliina.storage.StorageLong
import com.malliina.util.Util
import org.apache.commons.io.IOUtils
import org.scalatest.FunSuite

/**
 * @author Michael
 */
class StreamTests extends FunSuite {
  val path = Paths.get("conf/guitar-32x32.png")
  val file = path.toFile

  test("InputStream to Array[Byte]") {
    Util.using(new FileInputStream(file))(stream => {
      val bytes = IOUtils.toByteArray(stream)
      assert(bytes.length === Files.size(path).toInt)
    })
  }
  test("RangedInputStream to Array[Byte]") {
    val fiveTo14 = Util.using(new RangedInputStream(new FileInputStream(file), 5, 10))(stream => {
      val bytes = IOUtils.toByteArray(stream)
      assert(bytes.length === 10)
      bytes.toSeq
    })

    val tenTo19 = Util.using(new RangedInputStream(new FileInputStream(file), 10, 10))(stream => {
      val bytes = IOUtils.toByteArray(stream)
      assert(bytes.length === 10)
      bytes.toSeq
    })
    assert(fiveTo14.drop(5) === tenTo19.take(5))
  }
  test("Ranged for all") {
    val fileSize = Files.size(path).bytes
    val range = ContentRange.all(fileSize)
    assert(range.isAll)
    val allRange = Util.using(new RangedInputStream(new FileInputStream(file), range))(stream => {
      IOUtils.toByteArray(stream)
    })
    val fileRange = Util.using(new FileInputStream(file))(stream => IOUtils.toByteArray(stream))
    assert(allRange.length === fileRange.length)
    assert(allRange === fileRange)
  }
}
