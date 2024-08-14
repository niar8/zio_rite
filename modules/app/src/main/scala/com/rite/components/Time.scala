package com.rite.components

import scala.scalajs.*
import scala.scalajs.js.* // js native
import scala.scalajs.js.annotation.*

@js.native
@JSGlobal
class Moment extends js.Object {
  def format(): String  = js.native
  def fromNow(): String = js.native
}

@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {
  def unix(millis: Long): Moment = js.native
}

// API that will be use in the app
object Time {
  def unix2hr(millis: Long): String =
    MomentLib.unix(millis / 1000).fromNow()
}
