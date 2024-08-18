package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.core.Session
import com.rite.core.ZJS.*
import com.rite.http.requests.UpdatePasswordRequest
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import zio.*

final case class ChangePasswordState(
    password: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val emptyPasswordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")
  private val emptyNewPasswordError: Option[String] =
    Option.when(newPassword.isEmpty)("New password can't be empty")
  private val confirmPasswordError: Option[String] =
    Option.when(newPassword != confirmPassword)("Passwords must match")

  override val errorList: List[Option[String]] =
    List(emptyPasswordError, emptyNewPasswordError, confirmPasswordError) ++
      upstreamStatus.map(_.left.toOption).toList

  override def maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object ChangePasswordPage extends FormPage[ChangePasswordState](title = "Change Password") {
  override protected def basicState: ChangePasswordState = ChangePasswordState()

  override protected def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] =
    Session.getUserState
      .map { token =>
        List(
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
            "Change password",
            onClick.preventDefault.mapTo(stateVar.now()) --> submitter(token.email)
          )
        )
      }
      .getOrElse(
        List(
          div(
            cls := "centered-text",
            "It seems you're not logged in yet"
          )
        )
      )

  private def submitter(email: String) = Observer[ChangePasswordState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.updatePasswordEndpoint(
          payload = UpdatePasswordRequest(email, state.password, state.newPassword)
        )
      ).as {
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Password successfully changed"))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Left(e.getMessage))
            )
          )
        }
      }.runJs
  }
}
