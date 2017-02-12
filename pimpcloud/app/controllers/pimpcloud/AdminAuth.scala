package controllers.pimpcloud

import akka.stream.Materializer
import play.api.mvc.Action
import play.api.mvc.Results.Ok

/** TODO remove this code; seems to wrap unnecessarily.
  */
class AdminAuth(auth: PimpAuth, oauth: AdminOAuth, tags: CloudTags, val mat: Materializer) {
  // HTML
  def logout = auth.authAction(_ => oauth.eject.withNewSession)

  def eject = auth.logged(Action(req => Ok(tags.eject(req.flash.get(oauth.messageKey)))))
}
