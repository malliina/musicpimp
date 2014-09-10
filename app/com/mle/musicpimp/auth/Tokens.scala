package com.mle.musicpimp.auth

import java.nio.file.Path

import com.mle.musicpimp.util.{FileBackedList, FileUtil}
import com.mle.util.FileUtilities

/**
 * @author Michael
 */
class Tokens(file: Path) extends FileBackedList[Token](file)

class TokensStore extends controllers.TokenStore {
  val tokensFile = FileUtilities.pathTo("tokens.json")
  FileUtil.trySetOwnerOnlyPermissions(tokensFile)
  val tokens = new Tokens(tokensFile)

  override def persist(token: Token): Unit = tokens add token

  override def remove(token: Token): Unit = tokens remove token

  override def removeAll(user: String): Unit = removeWhere(_.user == user)

  override def remove(user: String, series: Long): Unit = removeWhere(t => t.user == user && t.series == series)

  def removeWhere(p: Token => Boolean) = tokens.get() filter p foreach remove

  override def findToken(user: String, series: Long): Option[Token] =
    tokens.get().find(t => t.user == user && t.series == series)
}