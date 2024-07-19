package com.rite.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rite.domain.data.*

trait ReviewRepository extends Repository[Review] {
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given reviewSchemaMeta: SchemaMeta[Review] = schemaMeta("reviews")
  inline given reviewInsertMeta: InsertMeta[Review] = insertMeta(_.id, _.created, _.updated)
  inline given reviewUpdateMeta: UpdateMeta[Review] =
    updateMeta(_.id, _.companyId, _.userId, _.created)

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
}

object ReviewRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], ReviewRepository] = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](new ReviewRepositoryLive(_))
  }
}
