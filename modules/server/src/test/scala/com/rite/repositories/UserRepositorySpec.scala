package com.rite.repositories

import com.rite.domain.data.User
import zio.*
import zio.test.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private def md5(usPassword: String): String = {
    val md                  = java.security.MessageDigest.getInstance("MD5")
    val digest: Array[Byte] = md.digest(usPassword.getBytes)
    val bigInt              = new java.math.BigInteger(1, digest)
    val hashedPassword      = bigInt.toString(16).trim
    prependWithZeros(hashedPassword)
  }

  /** This uses a little magic in that the string I start with is a “format specifier,” and it
    * states that the string it returns should be prepended with blank spaces as needed to make the
    * string length equal to 32. Then I replace those blank spaces with the character `0`.
    */
  private def prependWithZeros(pwd: String): String =
    "%1$32s".format(pwd).replace(' ', '0')

  private val rightUser = User(
    id = 1L,
    email = "test@rite.com",
    hashedPassword = md5("password")
  )

  override val initScript: String = "sql/users.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserRepositorySpec")(
      test("create user") {
        for {
          repo    <- ZIO.service[UserRepository]
          created <- repo.create(rightUser)
        } yield assertTrue {
          created.id == rightUser.id &&
          created.email == rightUser.email &&
          created.hashedPassword == rightUser.hashedPassword
        }
      },
      test("update user") {
        for {
          repo    <- ZIO.service[UserRepository]
          created <- repo.create(rightUser)
          updated <- repo.update(
            created.id,
            _.copy(email = "test2@rite.com", hashedPassword = md5("password2"))
          )
        } yield assertTrue {
          updated.id == created.id &&
          updated.email == "test2@rite.com" &&
          updated.hashedPassword == md5("password2")
        }
      },
      test("get user by id and by email") {
        for {
          repo           <- ZIO.service[UserRepository]
          created        <- repo.create(rightUser)
          fetchedById    <- repo.getById(created.id)
          fetchedByEmail <- repo.getByEmail(created.email)
        } yield assertTrue {
          fetchedById.contains(created) && fetchedByEmail.contains(created)
        }
      },
      test("delete user") {
        for {
          repo      <- ZIO.service[UserRepository]
          user      <- repo.create(rightUser)
          _         <- repo.delete(user.id)
          maybeUser <- repo.getById(user.id)
        } yield assertTrue(maybeUser.isEmpty)
      }
    ).provide(
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      Scope.default
    )
}
