package com.malliina.play.http

import com.malliina.values.*
import play.api.mvc.PathBindable

object Bindables extends Bindables

trait Bindables:
  implicit val username: PathBindable[Username] = bindable[Username](Username.unsafe)
  implicit val password: PathBindable[Password] = bindable[Password](Password.unsafe)
  implicit val email: PathBindable[Email] = bindable[Email](Email.unsafe)
  implicit val accessToken: PathBindable[AccessToken] = bindable[AccessToken](AccessToken.unsafe)
  implicit val idToken: PathBindable[IdToken] = bindable[IdToken](IdToken.unsafe)
  implicit val userId: PathBindable[UserId] =
    PathBindable.bindableLong.transform(l => UserId.unsafe(l), u => u.id)

  def bindable[T <: WrappedString](build: String => T): PathBindable[T] =
    PathBindable.bindableString.transform[T](s => build(s), u => u.value)
