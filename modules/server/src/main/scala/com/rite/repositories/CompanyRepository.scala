package com.rite.repositories

import com.rite.domain.data.*
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

trait CompanyRepository extends Repository[Company] {
  def getBySlug(slug: String): Task[Option[Company]]
  def uniqueAttributes: Task[CompanyFilter]
  def searchByFilter(filter: CompanyFilter): Task[List[Company]]
}

class CompanyRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {
  import quill.*

  inline given companySchemaMeta: SchemaMeta[Company] = schemaMeta("companies")
  inline given companyInsertMeta: InsertMeta[Company] = insertMeta(_.id)
  inline given companyUpdateMeta: UpdateMeta[Company] = updateMeta(_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(c => c)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getAll: Task[List[Company]] =
    run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Couldn't update: missing id $id"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(c => c)
      }
    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(c => c)
    }

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def uniqueAttributes: Task[CompanyFilter] =
    for {
      locations  <- run(query[Company].map(_.location).distinct).map(_.flatMap(_.toList))
      countries  <- run(query[Company].map(_.country).distinct).map(_.flatMap(_.toList))
      industries <- run(query[Company].map(_.industry).distinct).map(_.flatMap(_.toList))
      tags       <- run(query[Company].map(_.tags)).map(_.flatten.toSet.toList)
    } yield CompanyFilter(locations, countries, industries, tags)

  override def searchByFilter(filter: CompanyFilter): Task[List[Company]] =
    if (filter.isEmpty)
      getAll
    else
      run {
        query[Company]
          .filter { company =>
            liftQuery(filter.locations.toSet).contains(company.location) ||
            liftQuery(filter.countries.toSet).contains(company.country) ||
            liftQuery(filter.industries.toSet).contains(company.industry) ||
            sql"${lift(filter.tags)} && ${company.tags}".asCondition
          }
      }
}

object CompanyRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], CompanyRepository] = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](new CompanyRepositoryLive(_))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {
  val program: RIO[CompanyRepository, Unit] = for {
    repo <- ZIO.service[CompanyRepository]
    _    <- repo.create(Company(-1L, "test-test", "Test test", "test.com"))
  } yield ()

  override def run: Task[Unit] =
    program.provide(
      CompanyRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase), // quill instance
      Quill.DataSource.fromPrefix("rite.db")
    )
}
