package com.rite.repositories

import com.rite.domain.data.{Company, CompanyFilter}
import com.rite.syntax.*
import zio.*
import zio.test.*

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  private val testCompany = Company(-1L, "test-test", "Test test", "test.com")

  private def genString: String =
    scala.util.Random.alphanumeric.take(8).mkString

  private def genCompany: Company =
    Company(
      id = -1L,
      slug = genString,
      name = genString,
      url = genString,
      location = Some(genString),
      country = Some(genString),
      industry = Some(genString),
      tags = (1 to 3).map(_ => genString).toList
    )

  override val initScript: String = "sql/companies.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(testCompany)
        } yield company

        program.assert {
          case Company(_, "test-test", "Test test", "test.com", _, _, _, _, _) => true
          case _                                                               => false
        }
      },
      test("creating a duplicate should produce an error") {
        val program = for {
          repo <- ZIO.service[CompanyRepository]
          _    <- repo.create(testCompany)
          err  <- repo.create(testCompany).flip
        } yield err

        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(testCompany)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        }
      },
      test("update records") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(testCompany)
          updated     <- repo.update(company.id, _.copy(url = "newtest.com"))
          fetchedById <- repo.getById(company.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("delete a company") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(testCompany)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("get all companies") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          createdCompanies <- ZIO.foreach(1 to 10)(_ => repo.create(genCompany))
          fetchedCompanies <- repo.getAll
        } yield (createdCompanies, fetchedCompanies)

        program.assert { case (createdCompanies, fetchedCompanies) =>
          createdCompanies.toSet == fetchedCompanies.toSet
        }
      },
      test("search by tag") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(genCompany)
          fetched <- repo.searchByFilter(CompanyFilter(tags = company.tags.headOption.toList))
        } yield (fetched, company)

        program.assert { case (fetched, company) =>
          fetched.nonEmpty && fetched.tail.isEmpty && fetched.head == company
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      Scope.default
    )
}
