package com.rite.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import frontroute.*
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

object NotFoundPage {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div("404 - Invalid page")
}
