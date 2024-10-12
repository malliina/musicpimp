package com.malliina.play.http

import com.malliina.values._
import play.api.mvc.PathBindable

object Bindables extends Bindables

trait Bindables {
  implicit val username: PathBindable[Username] = bindable[Username](Username.apply)
  implicit val password: PathBindable[Password] = bindable[Password](Password.apply)
  implicit val email: PathBindable[Email] = bindable[Email](Email.apply)
  implicit val accessToken: PathBindable[AccessToken] = bindable[AccessToken](AccessToken.apply)
  implicit val idToken: PathBindable[IdToken] = bindable[IdToken](IdToken.apply)
  implicit val userId: PathBindable[UserId] =
    PathBindable.bindableLong.transform(l => UserId(l), u => u.id)

  def bindable[T <: WrappedString](build: String => T): PathBindable[T] =
    PathBindable.bindableString.transform[T](s => build(s), u => u.value)
}
