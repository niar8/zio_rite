package com.rite.integration

import com.rite.config.{JWTConfig, RecoveryTokensConfig}
import com.rite.domain.data.{Company, UserToken}
import com.rite.http.controllers.*
import com.rite.http.requests.*
import com.rite.http.responses.UserResponse
import com.rite.repositories.Repository.dataSourceLayer
import com.rite.repositories.*
import com.rite.services.*
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.test.*
import zio.json.*
import com.rite.syntax.*

import scala.collection.mutable

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val testEmail       = "rite@rite.com"
  private val testPassword    = "test"
  private val anotherPassword = "newtest"

  private val initialLoginRequest = LoginRequest(testEmail, testPassword)
  private val anotherLoginRequest = LoginRequest(testEmail, anotherPassword)
  private val registerRequest     = RegisterUserAccountRequest(testEmail, testPassword)

  private def backendStubZIO: URIO[JWTService & UserService, SttpBackend[Task, Any]] =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  private class EmailServiceProbe extends EmailService {
    private val db: mutable.Map[String, String] = mutable.Map.empty
    override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
      ZIO.unit
    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db += (to -> token))
    override def sendReviewInvite(from: String, to: String, company: Company): Task[Unit] =
      ZIO.unit

    // specific to the test
    def probeToken(email: String): Task[Option[String]] = ZIO.succeed(db.get(email))
  }

  private val emailServiceLayer: ULayer[EmailServiceProbe] =
    ZLayer.succeed(new EmailServiceProbe)

  override val initScript: String = "sql/integration.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("create user") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse]("/users", registerRequest)
        } yield assertTrue(maybeResponse.contains(UserResponse(testEmail)))
      },
      test("create and login") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse]("/users", registerRequest)
          maybeToken    <- backendStub.post[UserToken]("/users/login", initialLoginRequest)
        } yield assertTrue(
          maybeToken.exists(_.email == testEmail)
        )
      },
      test("change password") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse]("/users", registerRequest)
          userToken <- backendStub
            .post[UserToken]("/users/login", initialLoginRequest)
            .someOrFail(new RuntimeException("Authentication failed"))
          updatePasswordRequest = UpdatePasswordRequest(testEmail, testPassword, anotherPassword)
          _ <- backendStub
            .putAuth[UserResponse]("/users/password", updatePasswordRequest, userToken.token)
          maybeOldToken <- backendStub.post[UserToken]("/users/login", initialLoginRequest)
          maybeNewToken <- backendStub.post[UserToken]("/users/login", anotherLoginRequest)
        } yield assertTrue {
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        }
      },
      test("delete user") {
        for {
          backendStub   <- backendStubZIO
          userRepo      <- ZIO.service[UserRepository]
          maybeResponse <- backendStub.post[UserResponse]("/users", registerRequest)
          maybeOldUser  <- userRepo.getByEmail(testEmail)
          userToken <- backendStub
            .post[UserToken]("/users/login", initialLoginRequest)
            .someOrFail(new RuntimeException("Authentication failed"))
          deleteRequest = DeleteAccountRequest(testEmail, testPassword)
          _ <- backendStub.deleteAuth[UserResponse]("/users", deleteRequest, userToken.token)
          maybeUser <- userRepo.getByEmail(testEmail)
        } yield assertTrue {
          maybeOldUser.exists(_.email == testEmail) && maybeUser.isEmpty
        }
      },
      test("recover password flow")(
        for {
          backendStub <- backendStubZIO
          _           <- backendStub.post[UserResponse]("/users", registerRequest)
          _ <- backendStub.postNoResponse("/users/forgot", ForgotPasswordRequest(testEmail))
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          token <- emailServiceProbe
            .probeToken(testEmail)
            .someOrFail(new RuntimeException("token was NOT emailed"))
          recoverPasswordRequest = RecoverPasswordRequest(testEmail, token, anotherPassword)
          _             <- backendStub.postNoResponse("users/recover", recoverPasswordRequest)
          maybeOldToken <- backendStub.post[UserToken]("/users/login", initialLoginRequest)
          maybeNewToken <- backendStub.post[UserToken]("/users/login", anotherLoginRequest)
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      )
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokenRepositoryLive.layer,
      emailServiceLayer,
      dataSourceLayer,
      Repository.quillLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      ZLayer.succeed(RecoveryTokensConfig(24 * 3600)),
      Scope.default
    )

}
