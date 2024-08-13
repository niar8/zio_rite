package com.rite.services

import zio.*
import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.rite.config.{Configs, JWTConfig}
import com.rite.domain.data.*
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.TypesafeConfig

import java.time.Instant

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {
  private val ISSUER         = "rite.com"
  private val algorithm      = Algorithm.HMAC512(jwtConfig.secret)
  private val CLAIM_USERNAME = "username"

  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant())
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      token      <- ZIO.attempt(makeJwt(user, now, expiration))
    } yield UserToken(user.id, user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserId] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserId(
          id = decoded.getSubject.toLong,
          email = decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId

  private def makeJwt(user: User, issuedAt: Instant, expiresAt: Instant) =
    JWT
      .create()
      .withIssuer(ISSUER)
      .withIssuedAt(issuedAt)
      .withExpiresAt(expiresAt)
      .withSubject(user.id.toString)
      .withClaim(CLAIM_USERNAME, user.email)
      .sign(algorithm)
}

object JWTServiceLive {
  val layer: URLayer[JWTConfig, JWTService] = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer: TaskLayer[JWTService] =
    Configs.makeConfigLayer[JWTConfig]("rite.jwt") >>> layer
}

object JWTServiceDemo extends ZIOAppDefault {
  val program: RIO[JWTService, Unit] = for {
    service <- ZIO.service[JWTService]
    token   <- service.createToken(User(1L, "rite@rite.com", "unimportant"))
    _       <- Console.printLine(token)
    userId  <- service.verifyToken(token.token)
    _       <- Console.printLine(userId.toString)
  } yield ()

  override def run: Task[Unit] =
    program.provide(JWTServiceLive.configuredLayer)
}
