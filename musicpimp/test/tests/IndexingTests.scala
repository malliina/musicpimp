package tests

import com.malliina.musicpimp.db.{Indexer, PimpDb}
import com.malliina.util.Utils
import org.scalatest.FunSuite

class IndexingTests extends FunSuite {
  ignore("indexing performance") {
    val (fileCount, indexDuration) = Utils.timed {
      new Indexer(PimpDb.test()).index().toBlocking.last
    }
    println(s"Indexed $fileCount files in $indexDuration")
  }
}
