package com.mle.musicpimp.auth

import java.nio.file.Path

import com.mle.musicpimp.util.FileUtil
import com.mle.play.auth.{Token, TokenStore}
import com.mle.play.io.FileBackedList
import com.mle.util.Utils

import scala.concurrent.Future

/**
 * @author Michael
 */
class Tokens(file: Path) extends FileBackedList[Token](file) {
  protected override def load(): Seq[Token] = Utils.opt[Seq[Token], Exception](super.load()) getOrElse Nil
}

class TokensStore(tokensFile: Path) extends TokenStore {
  FileUtil.trySetOwnerOnlyPermissions(tokensFile)
  val tokens = new Tokens(tokensFile)

  override def persist(token: Token): Future[Unit] = fut(tokens add token)

  override def remove(token: Token): Future[Unit] = fut(tokens remove token)

  override def removeAll(user: String): Future[Unit] = removeWhere(_.user == user)

  override def remove(user: String, series: Long): Future[Unit] = removeWhere(t => t.user == user && t.series == series)

  def removeWhere(p: Token => Boolean): Future[Unit] = fut(tokens.get() filter p foreach remove)

  override def findToken(user: String, series: Long): Future[Option[Token]] =
    fut(tokens.get().find(t => t.user == user && t.series == series))

  def fut[T](t: T) = Future.successful(t)
}
