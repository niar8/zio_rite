package com.rite.services

import zio.*
import zio.test.*

import com.rite.services.*
import com.rite.domain.data.*
import com.rite.config.JWTConfig

object JWTServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service   <- ZIO.service[JWTService]
          userToken <- service.createToken(User(1L, "rite@rite.com", "unimportant"))
          userId    <- service.verifyToken(userToken.token)
        } yield assertTrue {
          userId.id == 1L && userId.email == "rite@rite.com"
        }
      }
    ).provide(
      JWTServiceLive.layer,
      ZLayer.succeed(JWTConfig("secret", 3600))
    )
}
