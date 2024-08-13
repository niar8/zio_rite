package com.rite.http.controllers

import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.client3.*
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import zio.*
import zio.test.*
import zio.json.*
import com.rite.syntax.*
import com.rite.domain.data.*
import com.rite.http.requests.CreateCompanyRequest
import com.rite.services.{CompanyService, JWTService}

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val testCompany = Company(1, "test-test", "Test test", "test.com")

  private val companyServiceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(testCompany)
    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed { if (id == 1) Some(testCompany) else None }
    override def getAll: Task[List[Company]] =
      ZIO.succeed(List(testCompany))
    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed { if (slug == testCompany.slug) Some(testCompany) else None }
    override def getAllFilters: Task[CompanyFilter] =
      ZIO.succeed(CompanyFilter.empty)
    override def searchByFilter(filter: CompanyFilter): Task[List[Company]] =
      ZIO.succeed(List(testCompany))
  }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.id, user.email, "BIG ACCESS", Long.MaxValue))
    override def verifyToken(token: String): Task[UserId] =
      ZIO.succeed(UserId(1L, "rite@rite.com"))
  }

  private def backendStubZIO(
      endpointFun: CompanyController => ServerEndpoint[Any, Task]
  ): URIO[JWTService & CompanyService, SttpBackend[Task, Any]] =
    for {
      controller <- CompanyController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("create a company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Test test", "test.com").toJson)
            .header("Authorization", "Bearer BIG ACCESS")
            .send(backendStub)
        } yield response

        program.assert {
          _.tryParseTo[Company].contains(testCompany)
        }
      },
      test("get all companies") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response    <- basicRequest.get(uri"/companies").send(backendStub)
        } yield response

        program.assert {
          _.tryParseTo[List[Company]].contains(List(testCompany))
        }
      },
      test("get a company by its id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response    <- basicRequest.get(uri"/companies/1").send(backendStub)
        } yield response

        program.assert {
          _.tryParseTo[Company].contains(testCompany)
        }
      }
    ).provide(
      ZLayer.succeed(companyServiceStub),
      ZLayer.succeed(jwtServiceStub)
    )
}
