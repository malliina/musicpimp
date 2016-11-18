import com.malliina.play.http.CookiedRequest
import com.malliina.play.models.Username
import play.api.mvc.AnyContent

import scala.concurrent.Future

package object controllers {
  type PimpRequest = CookiedRequest[AnyContent, Username]

  def fut[T](body: T) = Future.successful(body)
}
