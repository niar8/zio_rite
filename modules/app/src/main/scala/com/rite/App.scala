package com.rite

import com.raquo.laminar.api.L.{*, given}
import com.rite.components.*
import frontroute.LinkHandler
import org.scalajs.dom

object App {
  private val app = div(
    Header(),
    Router()
  ).amend(LinkHandler.bind) // for internal links

  private val containerNode = dom.document.querySelector("#app")

  def main(args: Array[String]): Unit =
    render(containerNode, app)
}
