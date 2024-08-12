package com.rite.repositories

import com.rite.config.{Configs, RecoveryTokensConfig}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
import com.rite.domain.data.*

trait RecoveryTokenRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokenRepositoryLive private (
    tokenConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokenRepository {
  import quill.*

  inline given tokenSchemaMeta: SchemaMeta[PasswordRecoveryToken] = schemaMeta("recovery_tokens")
  inline given tokenInsertMeta: InsertMeta[PasswordRecoveryToken] = insertMeta()
  inline given tokenUpdateMeta: UpdateMeta[PasswordRecoveryToken] = updateMeta(_.email)

  override def getToken(email: String): Task[Option[String]] =
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }

  override def checkToken(email: String, token: String): Task[Boolean] =
    for {
      now <- Clock.instant
      isValid <- run {
        query[PasswordRecoveryToken].filter { rt =>
          rt.email == lift(email) &&
          rt.token == lift(token) &&
          rt.expiration > lift(now.toEpochMilli)
        }
      }.map(_.nonEmpty)
    } yield isValid

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  private def findToken(email: String): Task[Option[String]] =
    run {
      query[PasswordRecoveryToken].filter(_.email == lift(email))
    }.map(_.headOption.map(_.token))

  private def replaceToken(email: String): Task[String] =
    for {
      recoveryToken <- createToken(email)
      _ <- run {
        query[PasswordRecoveryToken]
          .updateValue(lift(recoveryToken))
          .returning(rt => rt)
      }
    } yield recoveryToken.token

  private def generateToken(email: String): Task[String] =
    for {
      recoveryToken <- createToken(email)
      _ <- run {
        query[PasswordRecoveryToken]
          .insertValue(lift(recoveryToken))
          .returning(rt => rt)
      }
    } yield recoveryToken.token

  private def createToken(email: String): Task[PasswordRecoveryToken] =
    for {
      token      <- randomUppercaseString(8)
      expiration <- ZIO.attempt(java.lang.System.currentTimeMillis() + tokenConfig.duration)
    } yield PasswordRecoveryToken(email, token, expiration)

  private def randomUppercaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)
}

object RecoveryTokenRepositoryLive {
  private type R = Quill.Postgres[SnakeCase] & UserRepository

  val layer: URLayer[RecoveryTokensConfig & R, RecoveryTokenRepository] = ZLayer {
    for {
      tokenConfig <- ZIO.service[RecoveryTokensConfig]
      quill       <- ZIO.service[Quill.Postgres[SnakeCase]]
      userRepo    <- ZIO.service[UserRepository]
    } yield new RecoveryTokenRepositoryLive(tokenConfig, quill, userRepo)
  }

  val configuredLayer: RLayer[R, RecoveryTokenRepository] =
    Configs.makeConfigLayer[RecoveryTokensConfig]("rite.recovery_tokens") >>> layer
}
