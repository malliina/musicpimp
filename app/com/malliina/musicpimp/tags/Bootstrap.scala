package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.Tags._

import scalatags.Text.all._

object Bootstrap extends Bootstrap

/** Scalatags for Twitter Bootstrap.
  */
trait Bootstrap {
  val FormSignin = "form-signin"
  val FormSigninHeading = "form-signin-heading"

  val Alert = "alert"
  val AlertDanger = "alert alert-danger"
  val AlertSuccess = "alert alert-success"
  val AlertWarning = "alert alert-warning"
  val Btn = "btn"
  val BtnDanger = "btn btn-danger"
  val BtnDefault = "btn btn-default"
  val BtnGroup = "btn-group"
  val BtnPrimary = "btn btn-primary"
  val BtnLg = "btn-lg"
  val BtnSm = "btn-sm"
  val BtnXs = "btn-xs"
  val BtnBlock = "btn-block"
  val Caret = "caret"
  val Collapse = "collapse"
  val ColMd2 = "col-md-2"
  val ColMd3 = "col-md-3"
  val ColMd4 = "col-md-4"
  val ColMd6 = "col-md-6"
  val ColMd8 = "col-md-8"
  val ColMd9 = "col-md-9"
  val ColMd10 = "col-md-10"
  val ColMd11 = "col-md-11"
  val ColMd12 = "col-md-12"
  val ColMdOffset2 = "col-md-offset-2"
  val ColMdOffset4 = "col-md-offset-4"
  val ColSm2 = "col-sm-2"
  val ColSm4 = "col-sm-4"
  val ColSm10 = "col-sm-10"
  val ColSmOffset2 = "col-sm-offset-2"
  val Container = "container"
  val ControlLabel = "control-label"
  val DataParent = "data-parent"
  val DataTarget = "data-target"
  val DataToggle = "data-toggle"
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
  val Navbar = "navbar"
  val NavbarBrand = "navbar-brand"
  val NavbarCollapse = "navbar-collapse"
  val NavbarForm = "navbar-form"
  val NavbarHeader = "navbar-header"
  val NavbarDefault = "navbar-default"
  val NavbarNav = "navbar-nav"
  val NavbarRight = "navbar-right"
  val NavbarStaticTop = "navbar-static-top"
  val NavbarText = "navbar-text"
  val NavbarToggle = "navbar-toggle"
  val PageHeader = "page-header"
  val PullLeft = "pull-left"
  val PullRight = "pull-right"
  val Row = "row"
  val Table = "table"
  val TableCondensed = "table-condensed"
  val TableHover = "table-hover"
  val TableResponsive = "table-responsive"
  val TableStriped = "table-striped"
  val TableStripedHover = s"$Table $TableStriped $TableHover"
  val TableStripedHoverResponsive = s"$TableStripedHover $TableResponsive"
  val TextDanger = "text-danger"
  val TextInfo = "text-info"
  val TextMuted = "text-muted"
  val TextPrimary = "text-primary"
  val TextSuccess = "text-success"
  val TextWarning = "text-warning"
  val UneditableInput = "uneditable-input"
  val VisibleLg = "visible-lg"
  val VisibleMd = "visible-md"
  val VisibleSm = "visible-sm"

  val nav = tag(Nav)

  val dataParent = attr(DataParent)
  val dataTarget = attr(DataTarget)
  val dataToggle = attr(DataToggle)

  def alertDanger(message: String) = alertDiv(AlertDanger, message)

  def alertSuccess(message: String) = alertDiv(AlertSuccess, message)

  def alertDiv(alertClass: String, message: String) =
    divClass(s"$Lead $alertClass", role := Alert)(message)

  def leadPara = pClass(Lead)

  def headerRow(clazz: String = ColMd12)(header: Modifier*) =
    rowColumn(clazz)(
      headerDiv(
        h1(header)
      )
    )

  def fullRow(inner: Modifier*) = rowColumn(ColMd12)(inner)

  def halfRow(inner: Modifier*) = rowColumn(ColMd6)(inner)

  def rowColumn(clazz: String)(inner: Modifier*) = row(divClass(clazz)(inner))

  def row = divClass(Row)

  def div4 = divClass(ColMd4)

  def div6 = divClass(ColMd6)

  def divContainer = divClass(Container)

  def formGroup = divClass(FormGroup)

  def headerDiv = divClass(PageHeader)

  def glyphIcon(glyphName: String) = iClass(glyphClass(glyphName))

  def glyphClass(glyphName: String) = s"$Glyphicon $Glyphicon-$glyphName"

  def hamburgerButton =
    button(`class` := NavbarToggle, dataToggle := Collapse, dataTarget := s".$NavbarCollapse")(
      spanClass(IconBar),
      spanClass(IconBar),
      spanClass(IconBar)
    )

  def defaultSubmitButton = submitButton(`class` := BtnDefault)

  def blockSubmitButton(more: Modifier*) =
    submitButton(`class` := s"$BtnPrimary $BtnBlock", more)

  def responsiveTable[T](entries: Seq[T])(headers: String*)(cells: T => Seq[Modifier]) =
    headeredTable(TableStripedHoverResponsive, headers.map(stringFrag))(
      tbody(entries.map(entry => tr(cells(entry))))
    )
}
