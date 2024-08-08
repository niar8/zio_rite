package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{HTMLAnchorElement, HTMLDivElement, HTMLLIElement}

import scala.scalajs.js.annotation.*
import scala.scalajs.js

object Header {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "container-fluid p-0",
      div(
        cls := "jvm-nav",
        div(
          cls := "container",
          navTag(
            cls := "navbar navbar-expand-lg navbar-light JVM-nav",
            div(
              cls := "container",
              renderLogo(),
              button(
                cls                                         := "navbar-toggler",
                `type`                                      := "button",
                htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
                htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
                htmlAttr("aria-controls", StringAsIsCodec)  := "navbarNav",
                htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
                htmlAttr("aria-label", StringAsIsCodec)     := "Toggle navigation",
                span(cls := "navbar-toggler-icon")
              ),
              div(
                cls    := "collapse navbar-collapse",
                idAttr := "navbarNav",
                ul(
                  cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3",
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )

  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      cls  := "navbar-brand",
      img(
        cls := "home-logo",
        src := logoImage,
        alt := "Rock the JVM"
      )
    )

  private def renderNavLinks(): Seq[ReactiveHtmlElement[HTMLLIElement]] =
    List(
      renderNavLink("Companies", "/companies"),
      renderNavLink("Log In", "/login"),
      renderNavLink("Sign up", "/signup")
    )

  private def renderNavLink(text: String, location: String): ReactiveHtmlElement[HTMLLIElement] =
    li(
      cls := "nav-item",
      Anchors.renderNavLink(text, location, cssClass = "nav-link jvm-item")
    )

  object Anchors {
    def renderNavLink(
        text: String,
        location: String,
        cssClass: String = ""
    ): ReactiveHtmlElement[HTMLAnchorElement] =
      a(
        href := location,
        cls  := cssClass,
        text
      )
  }
}
