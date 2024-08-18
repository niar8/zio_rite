package com.rite.repositories

import com.rite.domain.data.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

import java.time.Instant

trait ReviewRepository extends Repository[Review] {
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getSummary(companyId: Long): Task[Option[ReviewSummary]]
  def insertSummary(companyId: Long, summary: String): Task[ReviewSummary]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given reviewSchemaMeta: SchemaMeta[Review] = schemaMeta("reviews")
  inline given reviewInsertMeta: InsertMeta[Review] = insertMeta(_.id, _.created, _.updated)
  inline given reviewUpdateMeta: UpdateMeta[Review] =
    updateMeta(_.id, _.companyId, _.userId, _.created)

  inline given reviewSummarySchemaMeta: SchemaMeta[ReviewSummary] = schemaMeta("review_summaries")
  inline given reviewSummaryInsertMeta: InsertMeta[ReviewSummary] = insertMeta()
  inline given reviewSummaryUpdateMeta: UpdateMeta[ReviewSummary] = updateMeta()

  override def create(review: Review): Task[Review] =
    run {
      query[Review]
        .insertValue(lift(review))
        .returning(r => r)
    }

  override def getById(id: Long): Task[Option[Review]] =
    run {
      query[Review].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getAll: Task[List[Review]] =
    run(query[Review])

  override def getByCompanyId(companyId: Long): Task[List[Review]] =
    run(query[Review].filter(_.companyId == lift(companyId)))

  override def getByUserId(userId: Long): Task[List[Review]] =
    run(query[Review].filter(_.userId == lift(userId)))

  override def update(id: Long, op: Review => Review): Task[Review] =
    for {
      current <- getById(id).someOrFail(
        new RuntimeException(s"update review failed: missing id $id")
      )
      updated <- run {
        query[Review]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated

  override def delete(id: Long): Task[Review] =
    run {
      query[Review]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }

  override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
    run {
      query[ReviewSummary].filter(_.companyId == lift(companyId))
    }.map(_.headOption)

  override def insertSummary(companyId: Long, summary: String): Task[ReviewSummary] =
    getSummary(companyId).flatMap {
      case None =>
        run {
          query[ReviewSummary]
            .insertValue(lift(ReviewSummary(companyId, summary, Instant.now())))
            .returning(rs => rs)
        }
      case Some(_) =>
        run {
          query[ReviewSummary]
            .filter(_.companyId == lift(companyId))
            .updateValue(lift(ReviewSummary(companyId, summary, Instant.now())))
            .returning(rs => rs)
        }
    }
}

object ReviewRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], ReviewRepository] = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](new ReviewRepositoryLive(_))
  }
}

object ReviewRepositoryPlayground extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] = {
    val program: RIO[ReviewRepository, Unit] = for {
      repo     <- ZIO.service[ReviewRepository]
      _        <- repo.insertSummary(companyId = 1, "This is a first summary")
      summary1 <- repo.getSummary(companyId = 1)
      _        <- Console.printLine(summary1)
      _        <- repo.insertSummary(companyId = 1, "This is the second summary")
      summary2 <- repo.getSummary(companyId = 1)
      _        <- Console.printLine(summary2)
    } yield ()

    program.provide(
      ReviewRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}
