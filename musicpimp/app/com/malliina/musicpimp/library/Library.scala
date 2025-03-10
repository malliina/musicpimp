package com.malliina.musicpimp.library

import java.io.FileNotFoundException
import java.nio.file.{AccessDeniedException, Files, Path, Paths}

import org.apache.pekko.stream.Materializer
import com.malliina.audio.meta.SongMeta
import com.malliina.musicpimp.audio.{PimpEnc, TrackMeta}
import com.malliina.musicpimp.db.*
import com.malliina.musicpimp.library.Library.*
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.values.UnixPath
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger

import scala.collection.immutable
import scala.concurrent.stm.{Ref, atomic}

trait FileStreams:
  def dataTrackStream: LazyList[DataTrack]
  def folderStream: LazyList[DataFolder]

trait FileLibrary extends FileStreams:
  def trackFiles: LazyList[Path]
  def reloadFolders(): Unit
  def findAbsoluteNew(trackPath: UnixPath): Option[Path]
  def suggestAbsolute(relative: Path): Option[Path]
  def localize(tracks: Seq[TrackMeta]): Seq[LocalTrack]
  def songPathsRecursive: immutable.Iterable[Path]
  def tracksRecursive: Iterable[LocalTrack]

object Library:
  private val log = Logger(getClass)

  val RootId = FolderID(idFor(""))
  val EmptyPath = Paths.get("")

  def apply()(implicit mat: Materializer): Library = new Library()(mat)
  def idFor(in: String) = DigestUtils.md5Hex(in)
  def parent(p: Path) = folderId(Option(p.getParent).getOrElse(EmptyPath))
  def folderId(p: Path) = FolderID(idFor(UnixPath(p).path))
  def trackId(p: Path) = TrackID(idFor(UnixPath(p).path))

class Library()(implicit mat: Materializer) extends FileLibrary:
  private val rootFolders: Ref[Seq[Path]] = Ref(Settings.read)

  private def roots = rootFolders.single.get

  private def rootStream: LazyList[Path] = LazyList.from(roots)

  def reloadFolders(): Unit = setFolders(Settings.read)

  def setFolders(folders: Seq[Path]): Unit = atomic(txn => rootFolders.set(folders)(txn))

  def localize(tracks: Seq[TrackMeta]): Seq[LocalTrack] =
    tracks.flatMap(track => findMeta(track.relativePath))

  private def all(root: Path): Map[Path, Folder] =
    def recurse(folder: Folder, acc: Map[Path, Folder]): Map[Path, Folder] =
      if folder.dirs.isEmpty then acc
      else
        Map(
          folder.dirs
            .flatMap(dir =>
              items(dir).toSeq
                .flatMap(f => recurse(f, acc.updated(dir, f)))
            )*
        )

    Map(items(root).toSeq.flatMap(f => recurse(f, Map(EmptyPath -> f)))*)

  private def all(): Map[Path, Folder] = Map(roots.flatMap(all)*)

  def songPathsRecursive: immutable.Iterable[Path] =
    all().flatMap(pair => pair._2.files)

  def tracksRecursive: Iterable[LocalTrack] =
    songPathsRecursive.flatMap(findMeta)

  def trackFiles: LazyList[Path] = recursivePaths(audioFiles)

  private def tracksStream: LazyList[LocalTrack] = tracksPathInfo.distinct.flatMap(parseMeta)

  private def tracksPathInfo =
    rootStream.flatMap(root => audioFiles(root).map(f => PathInfo(root.relativize(f), root)))

  private def audioFiles(root: Path) =
    FileUtils.readableFiles(root).filter(_.getFileName.toString.endsWith("mp3"))

  def folderStream: LazyList[DataFolder] =
    recursivePaths(FileUtils.folders).distinct.map(DataFolder.fromPath)

  private def recursivePaths(rootMap: Path => LazyList[Path]) =
    rootStream.flatMap(root => rootMap(root).map(root.relativize))

  def dataTrackStream: LazyList[DataTrack] = tracksStream map toDataTrack

  private def toDataTrack(track: LocalTrack) =
    val id = track.id
    val parent = Option(track.relativePath.getParent)
      .map(p => FolderID(idFor(UnixPath(p).path))) getOrElse RootId
    DataTrack(
      id,
      track.title,
      track.artist,
      track.album,
      track.duration,
      track.size,
      track.path,
      parent
    )

  /** This method has a bug.
    *
    * @param trackId
    *   the music item id
    * @return
    *   the absolute path to the music item id, or None if no such track exists
    */
  def findAbsoluteLegacy(trackId: TrackID): Option[Path] =
    findPathInfo(Paths.get(PimpEnc.decodeId(trackId))).map(_.absolute)

  def findAbsoluteNew(trackPath: UnixPath): Option[Path] =
    findPathInfo(Paths.get(trackPath.path)).map(_.absolute)

  def suggestAbsolute(relative: Path): Option[Path] = roots.headOption.map(_.resolve(relative))

  private def meta(relative: Path): LocalTrack =
    localTrackFor(pathInfo(relative))

  private def localTrackFor(pi: PathInfo): LocalTrack =
    val meta = SongMeta.fromPath(pi.absolute, pi.root)
    val path = UnixPath(pi.relative)
    new LocalTrack(TrackID(idFor(path.path)), path, meta)

  def toLocal(track: TrackMeta) = meta(track.relativePath)

  private def findMeta(relative: Path): Option[LocalTrack] =
    findPathInfo(relative) flatMap parseMeta

  private def parseMeta(pi: PathInfo): Option[LocalTrack] =
    try
      // InvalidAudioFrameException, CannotReadException
      Option(localTrackFor(pi))
    catch
      case e: Exception =>
        log.debug(
          s"Unable to read file: ${pi.absolute}. The file will be excluded from the library.",
          e
        )
        None

  private def items(relative: Path): Option[Folder] =
    val sources = findPathInfo2(relative)
    if sources.isEmpty then None
    else Some(mergeContents(sources))

  private def mergeContents(sources: Seq[PathInfo]): Folder =
    sources.map(items).foldLeft(Folder.empty)(_ ++ _)

  private def items(pathInfo: PathInfo): Folder =
    tryReadFolder(FileUtils.listFiles(pathInfo.absolute, pathInfo.root))

  private def findPathInfo(relative: Path): Option[PathInfo] =
    roots
      .find(root => Files.isReadable(root.resolve(relative)))
      .map(root => PathInfo(relative, root))

  private def findPathInfo2(relative: Path): Seq[PathInfo] =
    roots
      .filter(root => Files.isReadable(root.resolve(relative)))
      .map(root => PathInfo(relative, root))

  private def pathInfo(relative: Path): PathInfo = findPathInfo(relative)
    .getOrElse(throw new FileNotFoundException(s"Root folder for $relative not found."))

  /** Some folders might have unsuitable permissions, throwing an exception when a read attempt is
    * made. Suppresses such AccessDeniedExceptions.
    *
    * @param f
    *   function that returns folder contents
    * @return
    *   the folder, or an empty folder if the folder could not be read
    */
  private def tryReadFolder(f: => Folder): Folder =
    try f
    catch case ade: AccessDeniedException => Folder.empty

case class PathInfo(relative: Path, root: Path):
  def absolute = root.resolve(relative)
