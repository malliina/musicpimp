package com.malliina.musicpimp.auth

import cats.effect.IO

import java.nio.file.Path
import com.malliina.io.FileBackedList
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.values.Username

import scala.util.Try

class Tokens(file: Path) extends FileBackedList[Token](file):
  protected override def load(): Seq[Token] =
    Try(super.load()).getOrElse(Nil)

class TokensStore(tokensFile: Path) extends TokenStore[IO]:
  FileUtil.trySetOwnerOnlyPermissions(tokensFile)
  val tokens = new Tokens(tokensFile)

  override def persist(token: Token): IO[Unit] = pure(tokens.add(token))

  override def remove(token: Token): IO[Unit] = pure(tokens.remove(token))

  override def removeAll(user: Username): IO[Unit] = removeWhere(_.user == user)

  override def remove(user: Username, series: Long): IO[Unit] =
    removeWhere(t => t.user == user && t.series == series)

  private def removeWhere(p: Token => Boolean): IO[Unit] = pure(
    tokens.get().filter(p).foreach(remove)
  )

  override def findToken(user: Username, series: Long): IO[Option[Token]] =
    pure(tokens.get().find(t => t.user == user && t.series == series))

  def pure[T](t: T) = IO.pure(t)
