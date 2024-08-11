package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import frontroute.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.Session
import com.rite.core.ZJS.*
import com.rite.http.requests.LoginRequest
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLElement}
import zio.*

final case class LoginFormState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val invalidEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val emptyPasswordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  override val errorList: List[Option[String]] =
    List(invalidEmailError, emptyPasswordError, upstreamError)

  // None because of the redirecting to another page anyway
  override def maybeSuccess: Option[String] = None
}

object LoginPage extends FormPage[LoginFormState](title = "Log in") {
  override protected val stateVar: Var[LoginFormState] =
    Var(LoginFormState())

  override protected def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] = List(
    renderInput(
      name = "Email",
      uid = "email-input",
      kind = "text",
      isRequired = true,
      plcHolder = "Your email",
      updateFn = (s, e) =>
        s.copy(
          email = e,
          upstreamError = None,
          showStatus = false
        )
    ),
    renderInput(
      name = "Password",
      uid = "password-input",
      kind = "password",
      isRequired = true,
      plcHolder = "Your password",
      updateFn = (s, p) =>
        s.copy(
          password = p,
          upstreamError = None,
          showStatus = false
        )
    ),
    button(
      `type` := "button",
      "Log in",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  private val submitter = Observer[LoginFormState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.loginEndpoint(
          payload = LoginRequest(state.email, state.password)
        )
      ).map { userToken =>
        Session.setUserState(userToken)
        stateVar.set(LoginFormState())
        BrowserNavigation.replaceState("/")
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
        }
      }.runJs
  }
}
