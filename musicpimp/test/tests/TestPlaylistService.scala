package tests

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{FolderID, PlaylistID, SavedPlaylist, TrackID}
import com.malliina.values.{UnixPath, Username}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class TestPlaylistService extends PlaylistService {

  val zero = 0.seconds

  override implicit def ec: ExecutionContext = ActorMaterializer()(ActorSystem("test")).executionContext

  val tracks1 = Seq(
    DataTrack.fromValues(TrackID("1"), "Aces High", "Iron Maiden", "Powerslave", 100, 1000000, UnixPath.Empty, FolderID("folder 1")),
    DataTrack.fromValues(TrackID("2"), "So What", "Pink", "Funhouse", 120, 2000000, UnixPath.Empty, FolderID("folder 2")),
    DataTrack.fromValues(TrackID("3"), "Under the Waves", "Pendulum", "Immersion", 234, 12356889, UnixPath.Empty, FolderID("folder 3")),
    DataTrack.fromValues(TrackID("4"), "Witchcraft", "Pendulum", "Immersion", 100, 1234567, UnixPath.Empty, FolderID("folder 3")),
    DataTrack.fromValues(TrackID("5"), "A Track", "Pendulum", "Immersion", 123, 3455789, UnixPath.Empty, FolderID("folder 3"))
  )

  val tracks2 = Seq(
    DataTrack.fromValues(TrackID("3"), "Under the Waves", "Pendulum", "Immersion", 234, 12356889, UnixPath.Empty, FolderID("folder 3"))
  )

  var playlists = Seq(
    SavedPlaylist(PlaylistID(1), "name 1", 65, zero, tracks1),
    SavedPlaylist(PlaylistID(2), "name 2", 1, zero, tracks2)
  )

  /**
    * @return the saved playlists of user
    */
  override def playlists(user: Username): Future[Seq[SavedPlaylist]] =
    Future.successful(playlists)

  /**
    * @param playlist playlist submission
    * @return a Future that completes when saving is done
    */
  override def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: Username): Future[PlaylistID] =
    Future.successful(PlaylistID(0))

  /**
    * @param id playlist id
    * @return the playlist with ID `id`
    */
  override def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]] =
    Future.successful(playlists.find(_.id == id))

  override def delete(id: PlaylistID, user: Username): Future[Unit] =
    Future.successful(())
}
