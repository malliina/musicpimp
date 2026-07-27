package tests

import cats.effect.IO
import com.malliina.http.UrlSyntax.url
import com.malliina.http.io.HttpClientF
import com.malliina.musicpimp.audio.{TrackJson, TrackMeta}
import com.malliina.musicpimp.db.*
import com.malliina.musicpimp.http4s.{ServerTools, TestServerSuite}
import com.malliina.musicpimp.json.JsonStrings
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.*
import com.malliina.storage.StorageInt
import com.malliina.values.UnixPath
import com.malliina.ws.HttpUtil
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Json}
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.http.MimeTypes.JSON

import scala.concurrent.duration.DurationInt

class PlaylistsTests extends TestServerSuite:
//  override def pimpOptions: InitOptions = TestOptions.default

  implicit val f: Codec[TrackMeta] =
    TrackJson.format(url"http://www.google.com")
  val trackId = TrackID("Test.mp3")
  val testTracks: Seq[TrackID] = Seq(trackId)

  test("add tracks"):
//    val lib: DatabaseLibrary[IO] = components.lib
    val folderId = FolderID("Testid")
    def lib = server().service.lib
    def trackInserts =
      lib.insertTracks(
        Seq(
          DataTrack(trackId, "Ti", "Ar", "Al", 10.seconds, 1.megs, UnixPath.Empty, folderId)
        )
      )

    val insertions = for
      _ <- lib.deleteTracks
      _ <- lib.deleteFolders
      foldersInserted <- lib.insertFolders(
        Seq(DataFolder(folderId, "Testfolder", UnixPath.Empty, folderId))
      )
      tracksInserted <- trackInserts
    yield (foldersInserted, tracksInserted)
    val (fsi, tsi) = insertions.unsafeRunSync()
    assertEquals(fsi, 1L)
    assertEquals(tsi, 1L)
    val maybeFolder = lib.folder(folderId).unsafeRunSync()
    assert(maybeFolder.isDefined)

  http.test("GET /playlists"): client =>
    fetchLists(client, server()).map: list =>
      assertEquals(1, 1)

  http.test("POST /playlists"): client =>
    def postPlaylist(in: PlaylistSubmission) =
      client
        .postJsonAs[PlaylistSavedMeta](
          server().baseHttpUrl.append("/playlists"),
          Json.obj(JsonStrings.PlaylistKey -> in.asJson),
          testHeaders
        )
        .map(_.id)
    val submission = PlaylistSubmission(None, "test playlist", testTracks)
    for
      newId <- postPlaylist(submission)
      list <- fetchLists(client, server())
      added = list.find(_.id == newId)
      _ = assertEquals(added.get.tracks.map(_.id), testTracks)
      updatedTracks = testTracks ++ testTracks
      updatedPlaylist = submission.copy(id = Option(newId), tracks = updatedTracks)
      updatedId <- postPlaylist(updatedPlaylist)
      _ = assertEquals(newId, updatedId)
      updatedList <- fetchLists(client, server())
      _ = assertEquals(updatedList.find(_.id == updatedId).get.tracks.map(_.id), updatedTracks)
      del <- client.postJson(
        server().baseHttpUrl.append("/playlists/delete/$id"),
        Json.obj(),
        testHeaders
      )
      _ = assertEquals(202, del.status)
      listAgain <- fetchLists(client, server())
      _ = assert(listAgain.isEmpty)
    yield added

  def fetchLists(http: HttpClientF[IO], server: ServerTools): IO[Seq[FullSavedPlaylist]] =
    http
      .getAs[FullSavedPlaylistsMeta](
        server.baseHttpUrl.append("/playlists"),
        testHeaders
      )
      .map(_.playlists)

  private def testHeaders = Map(
    AUTHORIZATION -> HttpUtil.authorizationValue(
      DoobieUserManager.defaultUser.name,
      DoobieUserManager.defaultPass.pass
    ),
    ACCEPT -> JSON
  )
