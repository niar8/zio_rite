package com.rite.http.controllers

import com.rite.domain.data.UserId
import com.rite.http.endpoints.InviteEndpoints
import com.rite.http.responses.InviteResponse
import com.rite.services.*
import sttp.tapir.server.ServerEndpoint
import zio.*

class InviteController(
    inviteService: InviteService,
    jwtService: JWTService,
    paymentService: PaymentService
) extends BaseController
    with InviteEndpoints {
  val addPack: ServerEndpoint[Any, Task] =
    addPackEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { token => req =>
        inviteService
          .addInvitePack(token.email, req.companyId)
          .map(_.toString)
          .either
      }

  val invite: ServerEndpoint[Any, Task] =
    inviteEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { token => req =>
        inviteService
          .sendInvites(token.email, req.companyId, receivers = req.emails)
          .map { nInvitesSent =>
            val status = if (nInvitesSent == req.emails.size) "ok" else "partial success"
            InviteResponse(status, nInvitesSent)
          }
          .either
      }

  val getByUserId: ServerEndpoint[Any, Task] =
    getByUserIdEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { token => _ =>
        inviteService.getByUserName(token.email).either
      }

  val addPackPromoted: ServerEndpoint[Any, Task] =
    addPackPromotedEndpoint
      .serverSecurityLogic[UserId, Task](jwtService.verifyToken(_).either)
      .serverLogic { token => req =>
        inviteService
          .addInvitePack(token.email, req.companyId)
          .flatMap { paymentService.createCheckoutSession(_, userName = token.email) }
          .someOrFail(new RuntimeException("Cannot create payment checkout session"))
          .map(_.getUrl) // the checkout session URL
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(addPack, invite, getByUserId, addPackPromoted)
}

object InviteController {
  private type R = PaymentService & JWTService & InviteService
  val makeZIO: URIO[R, InviteController] =
    for {
      inviteService  <- ZIO.service[InviteService]
      jwtService     <- ZIO.service[JWTService]
      paymentService <- ZIO.service[PaymentService]
    } yield new InviteController(inviteService, jwtService, paymentService)
}
