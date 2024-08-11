package com.rite.core

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import scala.scalajs.js.*
import com.rite.domain.data.UserToken

object Session {
  private val stateName: String            = "userState"
  val userStateVar: Var[Option[UserToken]] = Var(Option.empty)

  def isActive: Boolean = userStateVar.now().nonEmpty

  def setUserState(token: UserToken): Unit = {
    userStateVar.set(Some(token))
    Storage.set(stateName, token)
  }

  def loadUserState(): Unit = {
    // clears any expired token
    Storage
      .get[UserToken](stateName)
      .filter(_.expiration * 1000 < new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    // retrieve the user token (known to be valid)
    userStateVar.set(Storage.get[UserToken](stateName))
  }
}