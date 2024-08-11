package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.core.Session
import org.scalajs.dom.HTMLElement
import org.scalajs.dom

final case class LogoutPageState() extends FormState {
  override def errorList: List[Option[String]] = Nil
  override def showStatus: Boolean             = false
  override def maybeSuccess: Option[String]    = None
}

object LogoutPage extends FormPage[LogoutPageState](title = "Log out") {
  override protected val stateVar: Var[LogoutPageState] =
    Var(LogoutPageState())

  override protected def renderChildren(): List[ReactiveHtmlElement[HTMLElement]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "logout-status",
      "You have been successfully logged out"
    )
  )
}
