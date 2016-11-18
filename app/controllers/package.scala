import com.malliina.play.http.CookiedRequest
import com.malliina.play.models.Username
import play.api.mvc.AnyContent

package object controllers {
  type PimpRequest = CookiedRequest[AnyContent, Username]
}
