package com.rite.services

import com.rite.domain.data.{Company, User, UserId, UserToken}
import com.rite.repositories.{RecoveryTokenRepository, UserRepository}
import com.rite.services.{EmailService, JWTService, UserService, UserServiceLive}
import zio.*
import zio.test.*

import scala.collection.mutable

object UserServiceSpec extends ZIOSpecDefault {
  private val testUser = User(
    1L,
    "rite@rite.com",
    "1000:13A3F41FCA4B3F6FBAB9C62D96174D30698FD9289C5FA567:61A5599F2AA19B604C2F78EA7D6F969335060B7C39743CD5"
  )

  private val testPassword    = "test_password"
  private val anotherEmail    = "another@gmail.com"
  private val anotherPassword = "another_password"

  private val userRepoStubLayer = ZLayer.succeed {
    new UserRepository {
      private val db: mutable.Map[Long, User] = mutable.Map[Long, User](1L -> testUser)
      override def create(user: User): Task[User] =
        ZIO.succeed {
          db += (user.id -> user)
          user
        }
      override def update(id: Long, op: User => User): Task[User] =
        ZIO.attempt {
          val newUser = op(db(id))
          db += (newUser.id -> newUser)
          newUser
        }
      override def getById(id: Long): Task[Option[User]] =
        ZIO.succeed(db.get(id))
      override def getAll: Task[List[User]] =
        ZIO.succeed(db.values.toList)
      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))
      override def delete(id: Long): Task[User] =
        ZIO.attempt {
          val user = db(id)
          db -= id
          user
        }
    }
  }

  private val tokenRepoStubLayer = ZLayer.succeed {
    new RecoveryTokenRepository {
      private val db: mutable.Map[String, String] = mutable.Map.empty
      override def getToken(email: String): Task[Option[String]] =
        ZIO.attempt {
          val token = util.Random.alphanumeric.take(8).mkString.toUpperCase()
          db += (email -> token)
          Some(token)
        }
      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).contains(token))
    }
  }

  private val emailServiceStubLayer: ULayer[EmailService] = ZLayer.succeed {
    new EmailService {
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
        ZIO.unit
      override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
        ZIO.unit

      override def sendReviewInvite(from: String, to: String, company: Company): Task[Unit] =
        ZIO.unit
    }
  }

  private val jwtServiceStubLayer: ULayer[JWTService] = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.id, user.email, "BIG ACCESS", Long.MaxValue))
      override def verifyToken(token: String): Task[UserId] =
        ZIO.succeed(UserId(testUser.id, testUser.email))
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(testUser.email, testPassword)
          isValid <- service.verifyPassword(testUser.email, testPassword)
        } yield assertTrue(isValid && user.email == testUser.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          isValid <- service.verifyPassword(testUser.email, testPassword)
        } yield assertTrue(isValid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          isValid <- service.verifyPassword(testUser.email, anotherPassword)
        } yield assertTrue(!isValid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          isValid <- service.verifyPassword(anotherEmail, testPassword)
        } yield assertTrue(!isValid)
      },
      test("update password") {
        for {
          service <- ZIO.service[UserService]
          _       <- service.updatePassword(testUser.email, testPassword, anotherPassword)
          isOldPasswordValid <- service.verifyPassword(testUser.email, testPassword)
          isNewPasswordValid <- service.verifyPassword(testUser.email, anotherPassword)
        } yield assertTrue(isNewPasswordValid && !isOldPasswordValid)
      },
      test("delete not-existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(anotherEmail, anotherPassword).flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(testUser.email, anotherPassword).flip
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(testUser.email, testPassword)
        } yield assertTrue(user.email == testUser.email)
      }
    ).provide(
      userRepoStubLayer,
      jwtServiceStubLayer,
      emailServiceStubLayer,
      tokenRepoStubLayer,
      UserServiceLive.layer
    )
}
