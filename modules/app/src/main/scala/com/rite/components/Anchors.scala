package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLAnchorElement
import org.scalajs.dom

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
