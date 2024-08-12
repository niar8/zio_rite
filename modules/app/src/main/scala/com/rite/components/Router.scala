package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.pages.*
import org.scalajs.dom
import frontroute.*
import org.scalajs.dom.HTMLElement

object Router {
  def apply(): ReactiveHtmlElement[HTMLElement] =
    mainTag(
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("companies")) { CompaniesPage() },
          path("signup") { SignUpPage() },
          path("login") { LoginPage() },
          path("forgot") { ForgotPasswordPage() },
          path("profile") { ProfilePage() },
          path("recover") { RecoverPasswordPage() },
          path("logout") { LogoutPage() },
          noneMatched { NotFoundPage() }
        )
      )
    )
}
