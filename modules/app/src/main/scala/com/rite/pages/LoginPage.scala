package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import frontroute.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.core.ZJS.*
import com.rite.http.requests.LoginRequest
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement
import zio.*

object LoginPage {
  private case class State(
      email: String = "",
      password: String = "",
      upstreamError: Option[String] = None,
      showStatus: Boolean = false
  ) {
    private val invalidEmailError: Option[String] =
      Option.when(!email.matches(Constants.emailRegex))("User email is invalid")
    private val emptyPasswordError: Option[String] =
      Option.when(password.isEmpty)("Password can't be empty")

    private val errorList: Seq[Option[String]] =
      List(invalidEmailError, emptyPasswordError, upstreamError)

    val maybeError: Option[String] =
      errorList.find(_.isDefined).flatten.filter(_ => showStatus)

    val hasErrors: Boolean =
      errorList.exists(_.isDefined)
  }

  private val stateVar = Var(State())

  private val submitter = Observer[State] { state =>
    if (state.hasErrors)
      stateVar.update(_.copy(showStatus = true))
    else
      useBackend(
        _.user.loginEndpoint(LoginRequest(state.email, state.password))
      ).map { userToken =>
        // TODO set user token
        stateVar.set(State())
        BrowserNavigation.replaceState("/")
      }.tapError { e =>
        ZIO.succeed {
          stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
        }
      }.runJs
  }

  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constants.logoImage,
            alt := "Rock the JVM"
          )
        )
      ),
      div(
        cls := "col-md-7",
        // right
        div(
          cls := "form-section",
          div(cls := "top-section", h1(span("Log In"))),
          children <-- stateVar.signal
            .map(_.maybeError)
            .map(_.map(renderError))
            .map(_.toList),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
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
              "Log In",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            )
          )
        )
      )
    )

  private def renderError(error: String): ReactiveHtmlElement[HTMLDivElement] =
    div(cls := "page-status-errors", error)

  private def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      plcHolder: String,
      updateFn: (State, String) => State
  ): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type`      := kind,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := plcHolder,
            onInput.mapToValue --> stateVar.updater(updateFn)
          )
        )
      )
    )
}
