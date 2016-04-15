package tests

import com.malliina.musicpimp.models.{SavedPlaylist, PlaylistID, User}
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.library.{PlaylistSubmission, PlaylistService}

import scala.concurrent.Future

class TestPlaylistService extends PlaylistService {
  val tracks1 = Seq(
    DataTrack.fromValues("1", "Aces High", "Iron Maiden", "Powerslave", 100, 1000000, "folder 1"),
    DataTrack.fromValues("2", "So What", "Pink", "Funhouse", 120, 2000000, "folder 2"),
    DataTrack.fromValues("3", "Under the Waves", "Pendulum", "Immersion", 234, 12356889, "folder 3"),
    DataTrack.fromValues("4", "Witchcraft", "Pendulum", "Immersion", 100, 1234567, "folder 3"),
    DataTrack.fromValues("5", "A Track", "Pendulum", "Immersion", 123, 3455789, "folder 3")
  )

  val tracks2 = Seq(
    DataTrack.fromValues("3", "Under the Waves", "Pendulum", "Immersion", 234, 12356889, "folder 3")
  )

  var playlists = Seq(
    SavedPlaylist(PlaylistID(1), "name 1", tracks1),
    SavedPlaylist(PlaylistID(2), "name 2", tracks2)
  )

  /**
   * @return the saved playlists of user
   */
  override def playlists(user: User): Future[Seq[SavedPlaylist]] = Future.successful(playlists)

  /**
   * @param playlist playlist submission
   * @return a Future that completes when saving is done
   */
  override def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[PlaylistID] = {
    Future.successful(PlaylistID(0))
  }

  /**
   * @param id playlist id
   * @return the playlist with ID `id`
   */
  override def playlist(id: PlaylistID, user: User): Future[Option[SavedPlaylist]] = Future.successful(playlists.find(_.id == id))

  override def delete(id: PlaylistID, user: User): Future[Unit] = Future.successful(())
}
