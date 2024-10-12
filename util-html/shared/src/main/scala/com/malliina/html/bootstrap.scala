package com.malliina.html

trait BootstrapStrings {
  val FormSignin = "form-signin"
  val FormSigninHeading = "form-signin-heading"
  val NoGutters = "no-gutters"

  val Container = "container"
  val ContainerFluid = "container-fluid"
  val ControlLabel = "control-label"
  val Dropdown = "dropdown"
  val DropdownMenu = "dropdown-menu"
  val DropdownToggle = "dropdown-toggle"
  val FormControl = "form-control"
  val FormGroup = "form-group"
  val FormHorizontal = "form-horizontal"
  val Glyphicon = "glyphicon"
  val HasError = "has-error"
  val HelpBlock = "help-block"
  val HiddenXs = "hidden-xs"
  val IconBar = "icon-bar"
  val InputGroup = "input-group"
  val InputGroupAddon = "input-group-addon"
  val InputGroupBtn = "input-group-btn"
  val InputGroupLg = "input-group-lg"
  val InputMd = "input-md"
  val Jumbotron = "jumbotron"
  val Lead = "lead"
  val ListUnstyled = "list-unstyled"
  val Nav = "nav"
  val NavStacked = "nav nav-stacked"
  val NavTabs = "nav nav-tabs"
  val MrAuto = "mr-auto"

  val PageHeader = "page-header"
  val PullLeft = "pull-left"
  val PullRight = "pull-right"
  val Row = "row"

  val Caret = "caret"
  val Collapse = "collapse"

  object navbars {
    val CollapseWord = "collapse"
    val Navbar = "navbar"
    val ExpandLg = "navbar-expand-lg"
    val Light = "navbar-light"
    val BgLight = "bg-light"

    val Brand = "navbar-brand"
    val Collapse = "navbar-collapse"
    val Form = "navbar-form"
    val Header = "navbar-header"
    val Default = "navbar-default"
    val Nav = "navbar-nav"
    val Right = "navbar-right"
    val StaticTop = "navbar-static-top"
    val Text = "navbar-text"
    val Toggle = "navbar-toggle"
    val Toggler = "navbar-toggler"
    val TogglerIcon = "navbar-toggler-icon"
    val DefaultLight = s"$Navbar $ExpandLg $Light $BgLight"

    val defaultNavbarId = "navbarSupportedContent"

    val UneditableInput = "uneditable-input"
    val VisibleLg = "visible-lg"
    val VisibleMd = "visible-md"
    val VisibleSm = "visible-sm"
  }

}

trait BootstrapParts {

  trait Component extends MiniComponent {
    lazy val default = named("default")
  }

  trait MiniComponent {
    lazy val danger = named("danger")
    lazy val dark = named("dark")
    lazy val info = named("info")
    lazy val light = named("light")
    lazy val primary = named("primary")
    lazy val secondary = named("secondary")
    lazy val success = named("success")
    lazy val warning = named("warning")

    def named(name: String): String
  }

  object alert extends Component {
    val Alert = "alert"

    def named(name: String) = s"alert alert-$name"
  }

  object btn extends Component {
    lazy val Btn = "btn"
    lazy val group = "btn-group"
    lazy val lg = "btn-lg"
    lazy val sm = "btn-sm"
    lazy val block = "btn-block"

    def named(name: String) = s"btn btn-$name"
  }

  object btnOutline extends Component {
    override def named(name: String) = s"btn btn-outline-$name"
  }

  object text extends Component {
    lazy val muted = named("muted")
    lazy val white = named("white")

    override def named(name: String) = s"text-$name"
  }

  object bg extends MiniComponent {
    lazy val white = named("white")

    override def named(name: String) = s"bg-$name"
  }

  object bgGradient extends MiniComponent {
    override def named(name: String) = s"bg-gradient-$name"
  }

  val Col = col.Col

  object col extends ColPrefixed {
    val Col = "col"

    def width(num: String) = s"$Col-$num"

    object sm extends Prefixed("sm")
    object md extends Prefixed("md")
    object lg extends Prefixed("lg")
    object xl extends Prefixed("xl")

    abstract class Prefixed(prefix: String) extends ColPrefixed {
      def width(num: String) = s"$Col-$prefix-$num"

      object offset extends ColPrefixed {
        override def width(num: String) = s"offset-$prefix-$num"
      }

    }

  }

  trait ColPrefixed {
    lazy val two = width("2")
    lazy val four = width("4")
    lazy val six = width("6")
    lazy val eight = width("8")
    lazy val twelve = width("12")

    def width(num: String): String
  }

  object align {

    object items extends Aligned {
      override def named(name: String) = s"align-items-$name"
    }

    object self extends Aligned {
      override def named(name: String) = s"align-self-$name"
    }

  }

  object justify {

    object content extends Aligned {
      lazy val around = named("around")
      lazy val between = named("between")

      override def named(name: String) = s"justify-content-$name"
    }

  }

  trait Aligned {
    lazy val start = named("start")
    lazy val center = named("center")
    lazy val end = named("end")

    def named(name: String): String
  }

  object tables {
    lazy val Table = "table"
    lazy val responsive = named("responsive")
    lazy val striped = named("striped")
    lazy val hover = named("hover")
    lazy val sm = named("sm")

    lazy val stripedHover = s"$Table $striped $hover"
    lazy val stripedHoverResponsive = s"$stripedHover $responsive"
    lazy val defaultClass = s"$Table $striped $hover $sm"

    def named(name: String) = s"$Table-$name"
  }

}

/** Scalatags for Twitter Bootstrap.
  */
class Bootstrap[Builder, Output <: FragT, FragT](val tags: Tags[Builder, Output, FragT])
  extends BootstrapStrings
  with BootstrapParts {

  import tags._
  import tags.impl.all._

  val dataParent = data("parent")
  val dataTarget = data("target")
  val dataToggle = data("toggle")

  val nav = tag(Nav)

  object navbar {

    import navbars._

    def simple[V: AttrValue](
      home: V,
      appName: Modifier,
      navItems: Modifier,
      navClass: String = DefaultLight,
      navBarId: String = defaultNavbarId
    ) =
      basic(home, appName, ulClass(s"${navbars.Nav} $MrAuto")(navItems), navClass, navBarId)

    def basic[V: AttrValue](
      home: V,
      appName: Modifier,
      navContent: Modifier,
      navClass: String = DefaultLight,
      navBarId: String = defaultNavbarId
    ) =
      nav(`class` := navClass)(
        divClass(Container)(
          a(`class` := Brand, href := home)(appName),
          button(
            `class` := Toggler,
            dataToggle := CollapseWord,
            dataTarget := s"#$navBarId",
            aria.controls := navBarId,
            aria.expanded := False,
            aria.label := "Toggle navigation"
          )(
            spanClass(TogglerIcon)
          ),
          div(`class` := s"$CollapseWord ${navbars.Collapse}", id := navBarId)(
            navContent
          )
        )
      )
  }

  def alertDanger(message: String) = alertDiv(alert.danger, message)

  def alertSuccess(message: String) = alertDiv(alert.success, message)

  def alertDiv(alertClass: String, message: String) =
    divClass(s"$Lead $alertClass", role := alert.Alert)(message)

  def leadPara = pClass(Lead)

  def headerRow(header: Modifier, clazz: String = col.md.twelve) =
    rowColumn(clazz)(
      headerDiv(
        h1(header)
      )
    )

  def fullRow(inner: Modifier*) = rowColumn(col.md.twelve)(inner)

  def halfRow(inner: Modifier*) = rowColumn(col.md.six)(inner)

  def rowColumn(clazz: String)(inner: Modifier*) = row(divClass(clazz)(inner))

  def row = divClass(Row)

  def div4 = divClass(col.md.four)

  def div6 = divClass(col.md.six)

  def divContainer = divClass(Container)

  def formGroup = divClass(FormGroup)

  def headerDiv = divClass(PageHeader)

  def defaultSubmitButton = submitButton(`class` := btn.default)

  def blockSubmitButton(more: Modifier*) =
    submitButton(`class` := s"${btn.primary} ${btn.block}", more)

  def responsiveTable[T](entries: Seq[T])(headers: String*)(cells: T => Seq[Modifier]) =
    headeredTable(tables.stripedHoverResponsive, headers.map(stringFrag))(
      tbody(entries.map(entry => tr(cells(entry))))
    )
}
