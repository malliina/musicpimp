package tests

import java.nio.file.Paths
import com.malliina.musicpimp.audio.PimpEnc.normalize
import io.circe.parser
import io.circe.syntax.EncoderOps

class Tests extends munit.FunSuite:

  test("encoding"):
    val input = "Svår (fålder)"
    assertEquals(normalize(input), "Svar (falder)")

  test("enc".ignore):
    val input = "artist/Svår (fålder)!"
//    assertEquals(makeIdentifier(input), "artist%2FSvar%20(falder)!")

  test("paths"):
    val root = Paths.get("a/b/c")
    val rel = Paths.get("")
    val combined = root.resolve(rel)
    assertEquals(root.toAbsolutePath.toString, combined.toAbsolutePath.toString)

  test("deconstruct array"):
    val arr = "a:b".split(":")
    arr match
      case Array(a, b) => assertEquals(a, "a")
      case _           => assertEquals(1, 2)

  test("for comp."):
    def eval(in: String) =
      def isA(input: String) = input == "a"

      val maybeV = Some(in)
      for actual <- maybeV if isA(actual) yield actual

    assert(eval("a").contains("a"))
    assert(eval("b").isEmpty)

  test("json"):
    val in = Seq("a", "b", "c")
    val jsV = Map("folders" -> in).asJson
    val jsString = jsV.noSpaces
    val list =
      parser.parse(jsString).flatMap(_.hcursor.downField("folders").as[Seq[String]]).getOrElse(Nil)
    assertEquals(in, list)

  test("serialize Option"):
    val jsValue = Some(42).asJson.noSpaces
    val none = Option.empty[Int].asJson.noSpaces
    assertEquals(jsValue, "42")
    assertEquals(none, "null")
