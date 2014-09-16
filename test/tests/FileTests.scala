package tests

import java.nio.file.Paths

import com.mle.musicpimp.library.Library
import com.mle.util.Utils
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationDouble

/**
 * @author Michael
 */
class FileTests extends FunSuite {
  val minFilesPerSecond = 400
  val testRoot = Paths get "E:\\musik"
  Library.rootFolders = Seq(testRoot)

  test(s"lists at least $minFilesPerSecond files per second or completes within 3 seconds") {
    val (size, duration) = Utils.timed(Library.trackFiles.size)
    val threshold = (1.0d * size / minFilesPerSecond).seconds
    assert(duration < 3.seconds || duration < threshold)
  }
  test("folder list") {
//    val folders = Library.folderStream
//    folders foreach println
  }
}
