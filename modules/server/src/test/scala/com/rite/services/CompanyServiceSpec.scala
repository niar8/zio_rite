package com.rite.services

import com.rite.domain.data.{Company, CompanyFilter}
import com.rite.http.requests.CreateCompanyRequest
import com.rite.repositories.CompanyRepository
import com.rite.services.{CompanyService, CompanyServiceLive}
import com.rite.syntax.*
import zio.*
import zio.test.*

import scala.collection.mutable

object CompanyServiceSpec extends ZIOSpecDefault {

  private val service: ZIO.ServiceWithZIOPartiallyApplied[CompanyService] =
    ZIO.serviceWithZIO[CompanyService]

  private val createCompanyReq = CreateCompanyRequest("Test test", "test.com")

  private val stubRepoLayer: ULayer[CompanyRepository] = ZLayer.succeed {
    new CompanyRepository {
      private val db: mutable.Map[Long, Company] = mutable.Map.empty
      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id = nextId)
          db += (nextId -> newCompany)
          newCompany
        }
      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db += (id -> op(company))
          company
        }
      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db -= id
          company
        }
      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))
      override def getAll: Task[List[Company]] =
        ZIO.succeed(db.values.toList)
      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))
      override def uniqueAttributes: Task[CompanyFilter] =
        ZIO.succeed {
          val companies  = db.values
          val locations  = companies.flatMap(_.location.toList).toSet.toList
          val countries  = companies.flatMap(_.country.toList).toSet.toList
          val industries = companies.flatMap(_.industry.toList).toSet.toList
          val tags       = companies.flatMap(_.tags).toSet.toList
          CompanyFilter(locations, countries, industries, tags)
        }
      override def searchByFilter(filter: CompanyFilter): Task[List[Company]] =
        ZIO.succeed {
          db.values.toList.filter { company =>
            filter.locations.isEmpty || filter.locations.contains(company.location) ||
            filter.countries.isEmpty || filter.countries.contains(company.country) ||
            filter.industries.isEmpty || filter.industries.contains(company.industry) ||
            filter.tags.toSet.intersect(company.tags.toSet).nonEmpty
          }
        }
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyServiceTest")(
      test("create") {
        val program = service(_.create(createCompanyReq))
        program.assert { company =>
          company.name == "Test test" &&
          company.url == "test.com" &&
          company.slug == "test-test"
        }
      },
      test("get by id") {
        val program = for {
          company    <- service(_.create(createCompanyReq))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Test test" &&
            company.url == "test.com" &&
            company.slug == "test-test" &&
            company == companyRes
          case _ => false
        }
      },
      test("get by slug") {
        val program = for {
          company    <- service(_.create(createCompanyReq))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Test test" &&
            company.url == "test.com" &&
            company.slug == "test-test" &&
            company == companyRes
          case _ => false
        }
      },
      test("get all") {
        val program = for {
          company   <- service(_.create(createCompanyReq))
          company2  <- service(_.create(CreateCompanyRequest("Google", "google.com")))
          companies <- service(_.getAll)
        } yield (company, company2, companies)

        program.assert {
          case (company, company2, companies) =>
            companies.toSet == Set(company, company2)
          case _ => false
        }
      }
    ).provide(
      CompanyServiceLive.layer,
      stubRepoLayer
    )
}
