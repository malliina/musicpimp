package controllers

import com.malliina.util.Log
import play.api.mvc.EssentialAction

/**
 * @author mle
 */
class PimpAssets extends Log {
  def at(file: String) = EssentialAction(req => {
    log.debug(s"$file")
    controllers.Assets.at(path = "/public", file)(req)
  })
}
