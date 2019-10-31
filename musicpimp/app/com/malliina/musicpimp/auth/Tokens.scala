package com.malliina.musicpimp.auth

import java.nio.file.Path

import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.play.io.FileBackedList
import com.malliina.util.Utils
import com.malliina.values.Username

import scala.concurrent.{ExecutionContext, Future}

class Tokens(file: Path) extends FileBackedList[Token](file) {
  protected override def load(): Seq[Token] =
    Utils.opt[Seq[Token], Exception](super.load()) getOrElse Nil
}

class TokensStore(tokensFile: Path, val ec: ExecutionContext) extends TokenStore {
  FileUtil.trySetOwnerOnlyPermissions(tokensFile)
  val tokens = new Tokens(tokensFile)

  override def persist(token: Token): Future[Unit] = fut(tokens add token)

  override def remove(token: Token): Future[Unit] = fut(tokens remove token)

  override def removeAll(user: Username): Future[Unit] = removeWhere(_.user == user)

  override def remove(user: Username, series: Long): Future[Unit] = removeWhere(
    t => t.user == user && t.series == series
  )

  def removeWhere(p: Token => Boolean): Future[Unit] = fut(tokens.get() filter p foreach remove)

  override def findToken(user: Username, series: Long): Future[Option[Token]] =
    fut(tokens.get().find(t => t.user == user && t.series == series))

  def fut[T](t: T) = Future.successful(t)
}
