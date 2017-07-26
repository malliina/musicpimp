package controllers.pimpcloud

import play.api.mvc.{AbstractController, ControllerComponents}

/** TODO remove this code; seems to wrap unnecessarily.
  */
class AdminAuth(comps: ControllerComponents, auth: PimpAuth, oauth: AdminOAuth, tags: CloudTags)
  extends AbstractController(comps) {
  // HTML
  def logout = auth.authAction(_ => oauth.eject.withNewSession)

  def eject = auth.logged(Action(req => Ok(tags.eject(req.flash.get(oauth.messageKey)))))
}
