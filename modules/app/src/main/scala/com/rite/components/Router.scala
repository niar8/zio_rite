package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.pages.*
import org.scalajs.dom
import frontroute.*
import org.scalajs.dom.HTMLElement

object Router {
  val externalUrlBus: EventBus[String] = EventBus()

  def apply(): ReactiveHtmlElement[HTMLElement] =
    mainTag(
      onMountCallback { ctx =>
        externalUrlBus.events.foreach(url => dom.window.location.href = url)(ctx.owner)
      },
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("companies")) { CompaniesPage() },
          path("signup") { SignUpPage() },
          path("login") { LoginPage() },
          path("forgot") { ForgotPasswordPage() },
          path("profile") { ProfilePage() },
          path("change_password") { ChangePasswordPage() },
          path("recover") { RecoverPasswordPage() },
          path("logout") { LogoutPage() },
          path("post") { CreateCompanyPage() },
          path("company" / long) { companyId => CompanyPage(companyId) },
          noneMatched { NotFoundPage() }
        )
      )
    )
}
