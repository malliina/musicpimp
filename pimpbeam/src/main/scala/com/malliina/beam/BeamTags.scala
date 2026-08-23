package com.malliina.beam

import com.malliina.html.{Bootstrap, HtmlTags}
import com.malliina.musicpimp.html.TagPage
import org.http4s.Uri
import org.http4s.implicits.uri
import scalatags.Text.all.*
import scalatags.text.Builder

object BeamTags extends Bootstrap(HtmlTags):
  import tags.*

  given AttrValue[Uri] = attrValue(_.renderString)

  private def attrValue[T](f: T => String): AttrValue[T] =
    (t: Builder, a: Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(f(v)))

  val autoplay = attr("autoplay").empty
  val controls = attr("controls").empty
  val preload = attr("preload")

  def at(file: String): Uri = uri"/assets".addPath(file)

  def index = base("MusicBeamer")(
    divContainer(
      fullRow(
        headerDiv(
          h1("MusicBeamer ", small("Stream music from your mobile device to this PC."))
        )
      ),
      div(id := "initial", `class` := "row")(
        div4(
          p(id := "status", `class` := "lead")("Initializing...")
        )
      ),
      div(id := "splash", `class` := "hidden")(
        rowColumn("col-md-12 centered")(
          img(id := "qr", src := at("img/guitar.png"), `class` := "auto-height")
        ),
        row(
          div4(
            leadPara(
              "Get the ",
              strong("MusicPimp"),
              " app for ",
              a(href := "https://play.google.com/store/apps/details?id=org.musicpimp", "Android"),
              ", ",
              a(
                href := "http://www.amazon.com/gp/product/B00GVHTEJY/ref=mas_pm_musicpimp",
                "Kindle Fire"
              ),
              ", ",
              a(
                href := "http://www.windowsphone.com/s?appid=84cd9030-4a5c-4a03-b0ab-4d59c2fa7d42",
                "Windows Phone"
              ),
              ", or ",
              a(
                href := "http://apps.microsoft.com/windows/en-us/app/musicpimp/73b9a42c-e38a-4edf-ac7e-00672230f7b6",
                "Windows 8"
              ),
              "."
            )
          ),
          div4(
            leadPara("Scan the QR code above.")
          ),
          div4(
            leadPara("Start playback from your mobile device.")
          )
        )
      ),
      div(id := "playback", `class` := "hidden")(
        rowColumn(col.md.six + " " + col.md.offset.width("3"))(
          audio(id := "player", autoplay, preload := "none", controls)(
            "You need to update your browser to support this feature."
          )
        ),
        rowColumn(col.md.six + " " + col.md.offset.width("3"))(
          img(id := "cover", src := at("img/guitar.png"))
        )
      ),
      jsScript(at("js/player.js"))
    )
  )

  def base(title: String)(inner: Modifier) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        deviceWidthViewport,
        meta(
          name := "description",
          content := "MusicBeamer lets you stream music from your mobile device to any PC. Capture the displayed image with the MusicPimp app and start playback."
        ),
        meta(
          name := "keywords",
          content := "music,stream,musicbeamer,mp3,audio,online,musicpimp,media"
        ),
        link(rel := "shortcut icon", href := at("img/guitar-18x16.png")),
        link(
          rel := "stylesheet",
          href := "//netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css"
        ),
        link(rel := "stylesheet", href := at("css/footer.css")),
        link(rel := "stylesheet", href := at("css/player.css")),
        jsScript("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js")
      ),
      body(
        div(id := "wrap")(
          inner,
          div(id := "push")
        ),
        div(id := "footer")(
          divContainer(
            p(`class` := "muted credit pull-right")(
              "Inspired by ",
              a(href := "http://www.photobeamer.com", "PhotoBeamer"),
              ". Developed by ",
              a(href := "https://www.mskogberg.info", "Michael Skogberg"),
              "."
            )
          )
        )
      )
    )
  )
