package controllers

import play.api.libs.Files
import play.api.mvc._

import scala.concurrent.ExecutionContext

object Uploads {
  def appMultipart(ec: ExecutionContext) =
    BodyParsers.parse.multipartFormData.map(toLengthParser)(ec)

  def toLengthParser(data: MultipartFormData[Files.TemporaryFile]): MultipartFormData[Long] = {
    val partLengths = data.files.map(fp => fp.copy(ref = fp.ref.file.length()))
    data.copy(files = partLengths)
  }
}

class Uploads(parser: BodyParser[MultipartFormData[Long]])
  extends Controller {

  def up = Action(parser) { req =>
    val lengths = req.body.files.map(file => s"${file.filename}: ${file.ref}")
    Ok(lengths mkString "\n")
  }
}
