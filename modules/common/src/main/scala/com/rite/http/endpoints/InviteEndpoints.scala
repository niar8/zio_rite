package com.rite.http.endpoints

import com.rite.domain.data.*
import com.rite.http.requests.*
import com.rite.http.responses.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait InviteEndpoints extends Endpoints {
  // POST /invite/add { companyId }
  val addPackEndpoint =
    secureBaseEndpoint
      .tag("Invites")
      .name("add invites")
      .description("Get invite tokens")
      .in("invite" / "add")
      .post
      .in(jsonBody[InvitePackRequest])
      .out(stringBody)

  // POST /invite { companyId, [emails] }
  val inviteEndpoint =
    secureBaseEndpoint
      .tag("Invites")
      .name("invite")
      .description("Send people emails inviting them to leave a review")
      .in("invite")
      .post
      .in(jsonBody[InviteRequest])
      .out(jsonBody[InviteResponse])

  // GET /invite/all
  val getByUserIdEndpoint =
    secureBaseEndpoint
      .tag("Invites")
      .name("get by user id")
      .description("Get all active invite packs for a user")
      .get
      .in("invite" / "all")
      .out(jsonBody[List[InviteNamedRecord]])

  // TODO - paid endpoints
}
