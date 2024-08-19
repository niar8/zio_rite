package com.rite.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

import scala.scalajs.js.Date

object Footer {
  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      cls := "main-footer",
      div("-= Written in Scala with love =-"),
      div(s"© ${new Date().getFullYear()} all rights reserved.")
    )
}
