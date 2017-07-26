package controllers.pimpcloud

import java.nio.file.{Files => JFiles}

import play.api.libs.Files
import play.api.mvc._

object Uploads {
  def toLengthParser(data: MultipartFormData[Files.TemporaryFile]): MultipartFormData[Long] = {
    val partLengths = data.files.map(fp => fp.copy(ref = JFiles.size(fp.ref.path)))
    data.copy(files = partLengths)
  }
}

class Uploads(comps: ControllerComponents, parser: BodyParser[MultipartFormData[Long]])
  extends AbstractController(comps) {

  def up = Action(parser) { req =>
    val lengths = req.body.files.map(file => s"${file.filename}: ${file.ref}")
    Ok(lengths mkString "\n")
  }
}
