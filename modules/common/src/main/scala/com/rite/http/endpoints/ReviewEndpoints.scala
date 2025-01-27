package com.rite.http.endpoints

import com.rite.domain.data.{Review, ReviewSummary}
import com.rite.http.requests.CreateReviewRequest
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

trait ReviewEndpoints extends Endpoints {
  // POST /reviews { CreateReviewRequest }
  val createEndpoint = secureBaseEndpoint
    .tag("reviews")
    .name("create")
    .description("Create a review for a company")
    .in("reviews")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  // GET /reviews/id
  val getByIdEndpoint = baseEndpoint
    .tag("reviews")
    .name("getById")
    .description("Get a review by its id")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  // GET /reviews/company/id/summary
  val getSummaryEndpoint = baseEndpoint
    .tag("Reviews")
    .name("get summary by company Id")
    .description("Get current review summary for a company Id")
    .in("reviews" / "company" / path[Long]("id") / "summary")
    .get
    .out(jsonBody[Option[ReviewSummary]])

  // POST /reviews/company/id/summary
  val makeSummaryEndpoint = baseEndpoint
    .tag("Reviews")
    .name("generate summary by company id")
    .description("Trigger review summary creation for a company Id")
    .in("reviews" / "company" / path[Long]("id") / "summary")
    .post
    .out(jsonBody[Option[ReviewSummary]])

  // GET /reviews/company/id
  val getByCompanyIdEndpoint = baseEndpoint
    .tag("reviews")
    .name("getByCompanyId")
    .description("Get all reviews for a company by its id")
    .in("reviews" / "company" / path[Long]("id"))
    .get
    .out(jsonBody[List[Review]])

  // GET /reviews/user/id
  val getByUserIdEndpoint = baseEndpoint
    .tag("reviews")
    .name("getByUserId")
    .description("Get all reviews written by a user")
    .in("reviews" / "user" / path[Long]("id"))
    .get
    .out(jsonBody[List[Review]])
}
