package com.rite

import com.raquo.laminar.api.L.{*, given}
import com.rite.components.*
import com.rite.core.*
import frontroute.LinkHandler
import org.scalajs.dom

object App {
  private val app = div(
    onMountCallback(_ => Session.loadUserState()),
    Header(),
    Router(),
    Footer()
  ).amend(LinkHandler.bind) // for internal links

  private val containerNode = dom.document.querySelector("#app")

  def main(args: Array[String]): Unit =
    render(containerNode, app)
}
