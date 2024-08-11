package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.Session
import com.rite.core.ZJS.*
import com.rite.http.requests.RegisterUserAccountRequest
import com.rite.pages.LoginPage.{renderInput, stateVar}
import frontroute.*
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLElement}
import zio.ZIO

final case class SignUpFormState(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val invalidEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val emptyPasswordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(password != confirmPassword)("Passwords must match")

  override val errorList: List[Option[String]] =
    List(invalidEmailError, emptyPasswordError, confirmPasswordError) ++
      upstreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object SignUpPage extends FormPage[SignUpFormState](title = "Sign up") {
  override protected val stateVar: Var[SignUpFormState] =
    Var(SignUpFormState())

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
          upstreamStatus = None,
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
          upstreamStatus = None,
          showStatus = false
        )
    ),
    renderInput(
      name = "Confirm password",
      uid = "confirm-password-input",
      kind = "password",
      isRequired = true,
      plcHolder = "Confirm password",
      updateFn = (s, p) =>
        s.copy(
          confirmPassword = p,
          upstreamStatus = None,
          showStatus = false
        )
    ),
    button(
      `type` := "button",
      "Sign Up",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  private val submitter = Observer[SignUpFormState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.createEndpoint(
          payload = RegisterUserAccountRequest(state.email, state.password)
        )
      ).as {
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Account created! You can login now."))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
        }
      }.runJs
  }
}
