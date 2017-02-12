package com.malliina.pimpcloud.tags

import com.malliina.pimpcloud.tags.Tags._

import scalatags.Text.all._

object Bootstrap extends Bootstrap

/** Scalatags for Twitter Bootstrap.
  */
trait Bootstrap {
  val FormControl = "form-control"
  val FormSignin = "form-signin"
  val FormSigninHeading = "form-signin-heading"

  val Alert = "alert"
  val AlertSuccess = "alert alert-success"
  val AlertWarning = "alert alert-warning"
  val Btn = "btn"
  val BtnGroup = "btn-group"
  val BtnPrimary = "btn btn-primary"
  val BtnDefault = "btn btn-default"
  val BtnLg = "btn-lg"
  val BtnXs = "btn-xs"
  val BtnBlock = "btn-block"
  val Caret = "caret"
  val Collapse = "collapse"
  val ColMd3 = "col-md-3"
  val ColMd4 = "col-md-4"
  val ColMd6 = "col-md-6"
  val ColMd8 = "col-md-8"
  val ColMd9 = "col-md-9"
  val ColMd12 = "col-md-12"
  val ColMdOffset2 = "col-md-offset-2"
  val Container = "container"
  val DataTarget = "data-target"
  val DataToggle = "data-toggle"
  val Dropdown = "dropdown"
  val DropdownMenu = "dropdown-menu"
  val DropdownToggle = "dropdown-toggle"
  val Glyphicon = "glyphicon"
  val IconBar = "icon-bar"
  val InputGroup = "input-group"
  val InputGroupBtn = "input-group-btn"
  val InputGroupLg = "input-group-lg"
  val Jumbotron = "jumbotron"
  val ListUnstyled = "list-unstyled"
  val Nav = "nav"
  val NavStacked = "nav nav-stacked"
  val Navbar = "navbar"
  val NavbarBrand = "navbar-brand"
  val NavbarCollapse = "navbar-collapse"
  val NavbarHeader = "navbar-header"
  val NavbarDefault = "navbar-default"
  val NavbarNav = "navbar-nav"
  val NavbarRight = "navbar-right"
  val NavbarToggle = "navbar-toggle"
  val PageHeader = "page-header"
  val PullLeft = "pull-left"
  val PullRight = "pull-right"
  val Row = "row"
  val Table = "table"
  val TableHover = "table-hover"
  val TableStriped = "table-striped"
  val TableStripedHover = s"$Table $TableStriped $TableHover"
  val VisibleLg = "visible-lg"
  val VisibleMd = "visible-md"
  val VisibleSm = "visible-sm"

  val nav = tag(Nav)

  val dataTarget = attr(DataTarget)
  val dataToggle = attr(DataToggle)

  def headerRow(clazz: String = ColMd12)(header: Modifier*) =
    rowColumn(clazz)(
      divClass(PageHeader)(
        h1(header)
      )
    )

  def fullRow(inner: Modifier*) = rowColumn(ColMd12)(inner)

  def rowColumn(clazz: String)(inner: Modifier*) = row(divClass(clazz)(inner))

  def row = divClass(Row)

  def div4 = divClass(ColMd4)

  def divContainer = divClass(Container)

  def glyphIcon(glyphName: String) = iClass(s"$Glyphicon $Glyphicon-$glyphName")

  def hamburgerButton =
    button(`class` := NavbarToggle, dataToggle := Collapse, dataTarget := s".$NavbarCollapse")(
      spanClass(IconBar),
      spanClass(IconBar),
      spanClass(IconBar)
    )
}
