package com.rite.repositories

import com.rite.domain.data.{Review, User}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait UserRepository extends Repository[User] {
  def getByEmail(email: String): Task[Option[User]]
}

class UserRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends UserRepository:
  import quill.*

  inline given userSchemaMeta: SchemaMeta[User] = schemaMeta("users")
  inline given userInsertMeta: InsertMeta[User] = insertMeta(_.id)
  inline given userUpdateMeta: UpdateMeta[User] = updateMeta(_.id)

  override def create(user: User): Task[User] =
    run {
      query[User]
        .insertValue(lift(user))
        .returning(u => u)
    }

  override def getById(id: Long): Task[Option[User]] =
    run {
      query[User].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getAll: Task[List[User]] =
    run(query[User])

  override def getByEmail(email: String): Task[Option[User]] =
    run {
      query[User].filter(_.email == lift(email))
    }.map(_.headOption)

  override def update(id: Long, op: User => User): Task[User] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Can't get user id: $id not found"))
      updated <- run {
        query[User]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(u => u)
      }
    } yield updated

  override def delete(id: Long): Task[User] =
    run {
      query[User]
        .filter(_.id == lift(id))
        .delete
        .returning(u => u)
    }

object UserRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], UserRepository] = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](new UserRepositoryLive(_))
  }
}
