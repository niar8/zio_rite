package com.rite.repositories

import com.rite.domain.data.Company
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

trait CompanyRepository extends Repository[Company] {
  def getBySlug(slug: String): Task[Option[Company]]
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

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

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
