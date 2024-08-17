package com.rite.http

import com.rite.http.controllers.*
import com.rite.services.*
import sttp.tapir.server.ServerEndpoint
import zio.*

object HttpApi {
  private type R = ReviewService & CompanyService & UserService & JWTService & InviteService &
    PaymentService

  val endpointsZIO: URIO[R, List[ServerEndpoint[Any, Task]]] =
    makeControllers.map(gatherRoutes)

  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
    users     <- UserController.makeZIO
    invites   <- InviteController.makeZIO
  } yield List(health, companies, reviews, users, invites)
}
