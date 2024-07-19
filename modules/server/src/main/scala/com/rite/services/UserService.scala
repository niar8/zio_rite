package com.rite.services

import zio.*

import java.security.SecureRandom
import com.rite.domain.data.*
import com.rite.repositories.*

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]

  // JWT
  def generateToken(email: String, password: String): Task[Option[UserToken]]

  // password recovery flow
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
    jwtService: JWTService,
    emailService: EmailService,
    userRepo: UserRepository,
    tokenRepo: RecoveryTokenRepository
) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    UserServiceLive.Hasher.generateHash(password).flatMap { hashedPassword =>
      userRepo.create(User(id = -1L, email = email, hashedPassword = hashedPassword))
    }

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo.getByEmail(email)
      result <- existingUser match
        case Some(user) =>
          UserServiceLive.Hasher
            .validateHash(password, user.hashedPassword)
            .orElseSucceed(false)
        case None => ZIO.succeed(false)
    } yield result

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user, email $email doesn't exist"))
      isVerified <- UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      hashedPassword <- UserServiceLive.Hasher.generateHash(newPassword)
      updatedUser <- userRepo
        .update(existingUser.id, _.copy(hashedPassword = hashedPassword))
        .when(isVerified)
        .someOrFail(new RuntimeException(s"Could not update password for $email"))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user, email $email doesn't exist"))
      isVerified <- UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      updatedUser <- userRepo
        .delete(existingUser.id)
        .when(isVerified)
        .someOrFail(new RuntimeException(s"Could not update password for $email"))
    } yield updatedUser

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"cannot verify user $email"))
      isVerified <- UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      maybeToken <- jwtService.createToken(existingUser).when(isVerified)
    } yield maybeToken

  override def sendPasswordRecoveryToken(email: String): Task[Unit] =
    tokenRepo.getToken(email).flatMap {
      case Some(token) => emailService.sendPasswordRecoveryEmail(email, token)
      case None        => ZIO.unit
    }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException("Non-existent user"))
      tokenIsValid   <- tokenRepo.checkToken(email, token)
      hashedPassword <- UserServiceLive.Hasher.generateHash(newPassword)
      result <- userRepo
        .update(existingUser.id, _.copy(hashedPassword = hashedPassword))
        .when(tokenIsValid)
        .map(_.nonEmpty)
    } yield result
}

object UserServiceLive {
  private type R = UserRepository & RecoveryTokenRepository & EmailService & JWTService

  val layer: URLayer[R, UserService] = ZLayer {
    for {
      jwtService   <- ZIO.service[JWTService]
      emailService <- ZIO.service[EmailService]
      userRepo     <- ZIO.service[UserRepository]
      tokenRepo    <- ZIO.service[RecoveryTokenRepository]
    } yield new UserServiceLive(jwtService, emailService, userRepo, tokenRepo)
  }

  object Hasher {
    private val N_ITERATIONS: Int        = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val HASH_BYTE_SIZE: Int      = 24
    private val skf: SecretKeyFactory    = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    def generateHash(string: String): Task[String] = ZIO.attempt {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt).toString
      val hashBytes = pbkdf2(string.toCharArray, salt, N_ITERATIONS, HASH_BYTE_SIZE)
      s"$N_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(string: String, hash: String): Task[Boolean] = ZIO.attempt {
      val hashSegments = hash.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray, salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec = PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded
    }

    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(string: String): Array[Byte] =
      string.grouped(2).toArray.map { hexChar =>
        Integer.parseInt(hexChar, 16).toByte
      }

    // a(i) ^ b(i) for every i
    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }
  }
}

object UserServiceDemo extends ZIOAppDefault {
  val program: Task[Unit] = for {
    hashedPassword <- UserServiceLive.Hasher.generateHash("test_password")
    _              <- Console.printLine(hashedPassword)
    isValid <- UserServiceLive.Hasher.validateHash(
      "test_password",
      "1000:13A3F41FCA4B3F6FBAB9C62D96174D30698FD9289C5FA567:61A5599F2AA19B604C2F78EA7D6F969335060B7C39743CD5"
    )
    _ <- Console.printLine(isValid)
  } yield ()

  override def run: Task[Unit] = program
}
