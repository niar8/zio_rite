package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rite.common.Constants
import com.rite.components.Anchors
import com.rite.core.Session
import org.scalajs.dom.HTMLDivElement

object ProfilePage {
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
        div(
          cls := "form-section",
          child <-- Session.userStateVar.signal.map {
            case None    => renderInvalid()
            case Some(_) => renderContent()
          }
        )
      )
    )

  private def renderInvalid() =
    div(cls := "top-section", h1(span("Oops!")), "It seems you're not logged in")

  private def renderContent() =
    div(
      cls := "top-section",
      h1(span("Profile")),
      div(
        cls := "profile-section",
        h3(span("Account settings")),
        Anchors.renderNavLink(text = "Change password", location = "/change_password")
      )
    )
}
