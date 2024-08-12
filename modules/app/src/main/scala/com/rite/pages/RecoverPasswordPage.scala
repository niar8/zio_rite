package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.rite.core.ZJS.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.components.Anchors
import com.rite.http.requests.RecoverPasswordRequest
import org.scalajs.dom.HTMLElement
import org.scalajs.dom
import zio.ZIO

final case class RecoverPasswordState(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val invalidEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
  private val emptyTokenError: Option[String] =
    Option.when(token.isEmpty)("Token can't be empty")
  private val emptyNewPasswordError: Option[String] =
    Option.when(newPassword.isEmpty)("Password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(newPassword != confirmPassword)("Passwords must match")

  override val errorList: List[Option[String]] =
    List(invalidEmailError, emptyTokenError, emptyNewPasswordError, confirmPasswordError) ++
      upstreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object RecoverPasswordPage extends FormPage[RecoverPasswordState]("Recover password") {
  override protected def basicState: RecoverPasswordState = RecoverPasswordState()

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
      name = "Recovery token (from email)",
      uid = "token-input",
      kind = "text",
      isRequired = true,
      plcHolder = "Your token",
      updateFn = (s, t) =>
        s.copy(
          token = t,
          upstreamStatus = None,
          showStatus = false
        )
    ),
    renderInput(
      name = "New password",
      uid = "new-password-input",
      kind = "password",
      isRequired = true,
      plcHolder = "New password",
      updateFn = (s, p) =>
        s.copy(
          newPassword = p,
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
    ),
    Anchors.renderNavLink(
      text = "Need a password recovery token?",
      location = "/forgot",
      cssClass = "auth-link"
    )
  )

  private val submitter = Observer[RecoverPasswordState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.recoverPasswordEndpoint(
          payload = RecoverPasswordRequest(state.email, state.token, state.newPassword)
        )
      ).as {
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Password successfully reset. You can login now"))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
        }
      }.runJs
  }
}
