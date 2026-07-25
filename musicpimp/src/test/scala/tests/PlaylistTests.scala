package tests

import cats.effect.{IO, Ref}
import com.malliina.audio.PlaylistIndex
import com.malliina.musicpimp.audio.BasePlaylist

class PlaylistTests extends munit.CatsEffectSuite:
  test("Playlist index reacts to playlist reorganization"):
    val tracks = Seq("a", "b", "c", "d", "e")
    for
      playlist <- testPlaylist()
      _ <- playlist.reset(2, tracks)
      _ <- playlist.move(1, 2)
      idx <- playlist.index
      _ = assertEquals(idx, 1)
      _ <- playlist.move(3, 4)
      idx2 <- playlist.index
      _ = assertEquals(idx2, 1)
      _ <- playlist.setIndex(0)
      _ <- playlist.move(1, 2)
      idx3 <- playlist.index
      _ = assertEquals(idx3, 0)
      _ <- playlist.move(0, 4)
      idx4 <- playlist.index
      _ = assertEquals(idx4, 4)
      _ <- playlist.move(0, 1)
      idx5 <- playlist.index
      _ = assertEquals(idx5, 4)
      _ <- playlist.setIndex(2)
      _ <- playlist.move(3, 2)
      idx6 <- playlist.index
      _ = assertEquals(idx6, 3)
    yield assertEquals(1, 1)

  def testPlaylist(): IO[TestPlaylist] =
    for
      pos <- Ref.of[IO, Int](0)
      songs <- Ref.of[IO, Seq[String]](Nil)
    yield TestPlaylist(pos, songs)

  class TestPlaylist(p: Ref[IO, Int], ss: Ref[IO, Seq[String]]) extends BasePlaylist[IO, String]:
    override protected val pos: Ref[IO, PlaylistIndex] = p
    override protected val songs: Ref[IO, Seq[String]] = ss
