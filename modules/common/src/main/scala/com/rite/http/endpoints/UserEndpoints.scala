package com.rite.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

import com.rite.domain.data.UserToken
import com.rite.http.requests.*
import com.rite.http.responses.UserResponse

trait UserEndpoints extends Endpoints {
  // POST /users { email, password } -> { email }
  val createEndpoint = baseEndpoint
    .tag("users")
    .name("register a user")
    .description("Register a new user account with username and password")
    .in("users")
    .post
    .in(jsonBody[RegisterUserAccountRequest])
    .out(jsonBody[UserResponse])

  // PUT /users/password { email, oldPassword, newPassword } -> { email }
  val updatePasswordEndpoint = secureBaseEndpoint
    .tag("users")
    .name("update a password")
    .description("Update user password")
    .in("users" / "password")
    .put
    .in(jsonBody[UpdatePasswordRequest])
    .out(jsonBody[UserResponse])

  // DELETE /users { email, password } -> { email }
  val deleteEndpoint = secureBaseEndpoint
    .tag("users")
    .name("delete an account")
    .description("Delete a user account")
    .in("users")
    .delete
    .in(jsonBody[DeleteAccountRequest])
    .out(jsonBody[UserResponse])

  // POST /users/login { email, password } -> { email, token, expiration }
  val loginEndpoint = baseEndpoint
    .tag("users")
    .name("login")
    .description("Login and generate JWT token")
    .in("users" / "login")
    .post
    .in(jsonBody[LoginRequest])
    .out(jsonBody[UserToken])

  // POST /users/forgot { email } -> 200 OK
  val forgotPasswordEndpoint = baseEndpoint
    .tag("users")
    .name("forgot password endpoint")
    .description("Trigger email for password recovery")
    .in("users" / "forgot")
    .post
    .in(jsonBody[ForgotPasswordRequest])

  // POST /users/recover { email, token, newPassword }
  val recoverPasswordEndpoint = baseEndpoint
    .tag("users")
    .name("Recover password endpoint")
    .description("Set new password based on OTP")
    .in("users" / "recover")
    .post
    .in(jsonBody[RecoverPasswordRequest])
}
