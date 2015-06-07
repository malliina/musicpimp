package controllers

import com.mle.util.Log
import play.api.mvc.EssentialAction

/**
 * @author mle
 */
object PimpAssets extends Log {
  def at(file: String) = EssentialAction(req => {
    log.debug(s"$file")
    controllers.Assets.at(path = "/public", file)(req)
  })
}
