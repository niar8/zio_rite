package com.rite.http.endpoints

import com.rite.domain.data.*
import com.rite.http.requests.*
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*

trait CompanyEndpoints extends Endpoints {
  // POST /companies { CreateCompanyRequest }
  val createEndpoint = secureBaseEndpoint
    .tag("companies")
    .name("create")
    .description("create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  // GET /companies/id
  val getByIdEndpoint = baseEndpoint
    .tag("companies")
    .name("getById")
    .description("get company by its id or a slug")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])

  // GET /companies
  val getAllEndpoint = baseEndpoint
    .tag("companies")
    .name("getAll")
    .description("get all company listings")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  // GET /companies/filters
  val getAllFiltersEndpoint = baseEndpoint
    .tag("companies")
    .name("getAllFilters")
    .description("Get all possible filters")
    .in("companies" / "filters")
    .get
    .out(jsonBody[CompanyFilter])

  val searchByFilterEndpoint = baseEndpoint
    .tag("companies")
    .name("searchByFilter")
    .description("Search companies applying a filter")
    .in("companies" / "search")
    .post
    .in(jsonBody[CompanyFilter])
    .out(jsonBody[List[Company]])
}
