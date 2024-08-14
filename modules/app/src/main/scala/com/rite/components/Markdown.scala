package com.rite.components

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*

@js.native
@JSImport("showdown", JSImport.Default)
object MarkdownLib extends js.Object {

  // inner class name DOES matter
  @js.native
  class Converter extends js.Object {
    def makeHtml(text: String): String = js.native
  }
}

object Markdown {
  def toHtml(text: String): String =
    new MarkdownLib.Converter().makeHtml(text)
}
