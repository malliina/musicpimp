package com.mle.musicpimp.library

import java.nio.file.{SimpleFileVisitor, Files, FileVisitResult, Path}
import com.mle.util.Log
import java.nio.file.attribute.BasicFileAttributes

/**
 * @author Michael
 */
object FileUtils extends Log {
  /**
   * Returns the given files and directories immediately under the specified dir,
   * i.e. performs a non-recursive search.
   *
   * The returned paths are relativized against the supplied root.
   *
   * @param dir parent
   * @return Non-recursive files and dirs.
   */
  def listFiles(dir: Path, root: Path) = {
    val visitor = new MusicItemVisitor(dir, root)
    Files.walkFileTree(dir, visitor)
    Folder(visitor.dirs, visitor.files)
  }
}

class MusicItemVisitor(val startDir: Path, root: Path)
  extends SimpleFileVisitor[Path] with Log {
  private[this] var dirBuffer: List[Path] = Nil

  private[this] var fileBuffer: List[Path] = Nil

  def dirs = dirBuffer.reverse

  def files = fileBuffer.reverse

  override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
    if (attrs.isRegularFile && file.getFileName.toString.endsWith("mp3")) {
      fileBuffer = preparePath(file) :: fileBuffer
    }
    FileVisitResult.CONTINUE
  }

  override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = {
    if (Files.isSameFile(startDir, dir)) {
      FileVisitResult.CONTINUE
    } else {
      dirBuffer = preparePath(dir) :: dirBuffer
      FileVisitResult.SKIP_SUBTREE
    }
  }

  def preparePath(path: Path) = root relativize path
}
