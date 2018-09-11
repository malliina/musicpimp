package controllers

import java.net.ConnectException
import java.nio.file.Paths

import com.malliina.concurrent.Execution.cached
import com.malliina.http._
import com.malliina.oauth.DiscoGsOAuthCredentials
import com.malliina.play.http.Proxies
import controllers.Covers.log
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

object Covers {
  private val log = Logger(getClass)

  val tempDir = Paths.get(sys.props("java.io.tmpdir"))
}

class Covers(oauth: MetaOAuth,
             creds: DiscoGsOAuthCredentials,
             comps: ControllerComponents) extends AbstractController(comps) {
  val fallbackCoverDir = Covers.tempDir.resolve("covers")
  val coverDir = sys.props.get("cover.dir").fold(fallbackCoverDir)(path => Paths.get(path))
  val covers = DiscoClient(creds, coverDir)

  def ping = oauth.logged(Action(Ok))

  def cover = oauth.logged {
    Action.async { request =>
      def message(msg: String) = s"From '${Proxies.realAddress(request)}': $msg"

      def query(key: String) = (request getQueryString key).filter(_.nonEmpty)

      val result = for {
        artist <- query("artist")
        album <- query("album")
      } yield {
        val coverName = s"$artist - $album"
        covers.cover(artist, album).map { path =>
          log info message(s"Serving cover '$coverName' at '$path'.")
          Ok.sendFile(path.toFile)
        }.recover {
          case _: CoverNotFoundException =>
            val userMessage = s"Unable to find cover '$coverName'."
            log info message(userMessage)
            notFound(userMessage)
          case _: NoSuchElementException =>
            val userMessage = s"Unable to find cover '$coverName'."
            log info message(userMessage)
            notFound(userMessage)
          case re: ResponseException =>
            log.error(s"Invalid response received.", re)
            BadGateway
          case ce: ConnectException =>
            log.warn(message(s"Unable to search for cover '$coverName'. Unable to connect to cover backend: ${ce.getMessage}"), ce)
            BadGateway
          case t: Throwable =>
            log.error(message(s"Failure while searching cover '$coverName'."), t)
            InternalServerError
        }
      }
      result getOrElse Future.successful(BadRequest)
    }
  }

  def notFound(message: String) = NotFound(Json.toJson(Errors(Seq(SingleError(message)))))
}
