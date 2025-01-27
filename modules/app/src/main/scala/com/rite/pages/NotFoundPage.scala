package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

object NotFoundPage {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "simple-titled-page",
      h1("Oops!"),
      h2("This page does not exist")
    )
}
