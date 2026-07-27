package tests

import play.api.ApplicationLoader.Context
import play.api.mvc.Results.*
import play.api.routing.Router
import play.api.routing.sird.*
import play.api.test.WithApplicationLoader
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.filters.HttpFiltersComponents

class TrivialAppLoader
  extends WithApplicationLoader((context: Context) =>
    TrivialAppLoader.components(context).application
  )

object TrivialAppLoader:
  def components(context: Context) =
    new BuiltInComponentsFromContext(context) with HttpFiltersComponents:
      override val router: Router = Router.from:
        case GET(p"/") => defaultActionBuilder(Ok)
