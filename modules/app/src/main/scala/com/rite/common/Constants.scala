package com.rite.common

import scala.scalajs.js.annotation.*
import scala.scalajs.js

object Constants {
  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("/static/img/generic_company.png", JSImport.Default)
  val companyLogoPlaceholder: String = js.native

  val emailRegex: String =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""

  val urlRegex: String =
    """^(https?):\/\/(([^:/?#]+)(?::(\d+))?)(\/[^?#]*)?(\?[^#]*)?(#.*)?"""
}
