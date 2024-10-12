package controllers.pimpcloud

import java.nio.file.Files as JFiles

import play.api.libs.Files
import play.api.mvc.*

object Uploads:
  val a = 42
//  def toLengthParser(data: MultipartFormData[Files.TemporaryFile]): MultipartFormData[Long] =
//    val partLengths: Seq[MultipartFormData.FilePart[Files.TemporaryFile]] =
//      data.files.map(fp => fp.copy(fileSize = JFiles.size(fp.ref.path)))
//    data.copy(files = partLengths)

//class Uploads(comps: ControllerComponents, parser: BodyParser[MultipartFormData[Long]])
//  extends AbstractController(comps):
//
//  def up = Action(parser): (req: Request[MultipartFormData[Long]]) =>
//    val lengths = req.body.files.map(file => s"${file.filename}: ${file.ref}")
//    Ok(lengths.mkString("\n"))
