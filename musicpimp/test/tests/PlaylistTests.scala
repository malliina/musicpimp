package tests

import com.malliina.musicpimp.audio.BasePlaylist

import scala.concurrent.stm.Ref

class PlaylistTests extends munit.FunSuite {
  test("Playlist index reacts to playlist reorganization") {
    val tracks = Seq("a", "b", "c", "d", "e")
    val playlist = new TestPlaylist
    playlist.reset(2, tracks)
    playlist.move(1, 2)
    assert(playlist.index == 1)
    playlist.move(3, 4)
    assert(playlist.index == 1)
    playlist.index = 0
    playlist.move(1, 2)
    assert(playlist.index == 0)
    playlist.move(0, 4)
    assert(playlist.index == 4)
    playlist.move(0, 1)
    assert(playlist.index == 4)
    playlist.index = 2
    playlist.move(3, 2)
    assert(playlist.index == 3)
  }

  class TestPlaylist extends BasePlaylist[String] {
    override protected val pos: Ref[PlaylistIndex] = Ref[Int](0)
    override protected val songs: Ref[Seq[String]] = Ref[Seq[String]](Nil)
  }

}
