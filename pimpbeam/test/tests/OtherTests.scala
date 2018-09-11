package tests

import com.malliina.http.WebUtils
import org.scalatest.FunSuite

/**
  */
class OtherTests extends FunSuite {
  test("WebUtils.encodeURIComponent") {
    val artist = WebUtils.encodeURIComponent("iron maiden")
    val album = WebUtils.encodeURIComponent("somewhere in time")
    val myUri = s"http://api.discogs.com/database/search?artist=$artist&release_title=$album"
    val expected = "http://api.discogs.com/database/search?artist=iron%20maiden&release_title=somewhere%20in%20time"
    assert(myUri === expected)
  }
  //  def extension(file: String) = Try(file.substring(file.lastIndexOf('.') + 1)).toOption

  //  test("extension") {
  //    assert(DiscoGs.client.extension("file.mp3") === Some("mp3"))
  //  }
  //  test("can download album cover from DiscoGs") {
  //    val coverDownload = DiscoGs.client.coverFile("iron maiden", "somewhere in time")
  //    val (file, bytesDownloaded) = Await.result(coverDownload, 20 seconds)
  //    //    println(s"Downloaded $bytesDownloaded (${bytesDownloaded.toBytes} bytes) to: $file")
  //    Files.deleteIfExists(file)
  //    assert(bytesDownloaded.toBytes === 233664)
  //  }
  //  test("fetching a nonexistent album cover fails with a NoSuchElementException") {
  //    val failedResult = DiscoGs.client.coverPath("nonexistent artist", "nonexistent album").map(_ => false).recover {
  //      case nsee: NoSuchElementException => true
  //      case _: Throwable => false
  //    }
  //    val failedProperly = Await.result(failedResult, 10 seconds)
  //    assert(failedProperly)
  //  }

  //  test("api.musicpimp.org/covers") {
  //    val tmp = FileUtilities.tempDir / "cover.jpg"
  //    val dl = DiscoGs.coverFile("iron maiden", "somewhere in time", tmp)
  //    val size = Await.result(dl, 5.seconds)
  //    assert(size.toBytes === 233664)
  //  }
}
