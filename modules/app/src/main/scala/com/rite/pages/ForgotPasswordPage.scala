package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.ZJS.*
import com.rite.http.requests.ForgotPasswordRequest
import org.scalajs.dom.HTMLElement
import zio.ZIO

final case class ForgotPasswordState(
    email: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val invalidEmailError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("User email is invalid")

  override val errorList: List[Option[String]] =
    List(invalidEmailError) ++ upstreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamStatus.flatMap(_.toOption)
}

object ForgotPasswordPage extends FormPage[ForgotPasswordState]("Forgot password") {
  override protected val stateVar: Var[ForgotPasswordState] =
    Var(ForgotPasswordState())

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
    button(
      `type` := "button",
      "Recover password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  private val submitter = Observer[ForgotPasswordState] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.forgotPasswordEndpoint(
          payload = ForgotPasswordRequest(state.email)
        )
      ).as {
        stateVar.update(
          _.copy(
            showStatus = true,
            upstreamStatus = Some(Right("Check your email"))
          )
        )
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
        }
      }.runJs
  }
}
