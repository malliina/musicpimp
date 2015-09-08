package tests

import java.nio.file.Paths

import com.mle.musicpimp.library.Library
import org.scalatest.FunSuite

/**
 * @author Michael
 */
trait PimpTestSuite extends FunSuite {
  val testRoot = Paths get "E:\\musik"
  Library.rootFolders = Seq(testRoot)
  val testFolder = "All+Time+Top+1000"
}
