package com.rite.http.controllers

import com.rite.domain.data.UserId
import zio.*
import sttp.tapir.server.*
import com.rite.domain.errors.*
import com.rite.http.controllers.UserController.makeZIO
import com.rite.http.endpoints.UserEndpoints
import com.rite.http.requests.DeleteAccountRequest
import com.rite.services.{JWTService, UserService}
import com.rite.http.responses.UserResponse
import sttp.tapir.auth
import sttp.tapir.server.ServerEndpoint.Full

class UserController private (
    userService: UserService,
    jwtService: JWTService
) extends BaseController
    with UserEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic { req =>
      userService
        .registerUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] =
    loginEndpoint.serverLogic { req =>
      userService
        .generateToken(req.email, req.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { _ => req =>
        userService
          .updatePassword(req.email, req.oldPassword, req.newPassword)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { _ => req =>
        userService
          .deleteUser(req.email, req.password)
          .map(user => UserResponse(user.email))
          .either
      }

  val forgotPassword: ServerEndpoint[Any, Task] =
    forgotPasswordEndpoint.serverLogic { req =>
      userService.sendPasswordRecoveryToken(req.email).either
    }

  val recoverPassword: ServerEndpoint[Any, Task] =
    recoverPasswordEndpoint
      .serverLogic { req =>
        userService
          .recoverPasswordFromToken(req.email, req.token, req.newPassword)
          .filterOrFail(b => b)(UnauthorizedException)
          .unit
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(
      create,
      updatePassword,
      delete,
      login,
      forgotPassword,
      recoverPassword
    )
}

object UserController {
  val makeZIO: URIO[JWTService & UserService, UserController] =
    for {
      userService <- ZIO.service[UserService]
      jwtService  <- ZIO.service[JWTService]
    } yield new UserController(userService, jwtService)
}
