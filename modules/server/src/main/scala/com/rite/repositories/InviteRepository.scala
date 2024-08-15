package com.rite.repositories

import com.rite.domain.data.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait InviteRepository {
  def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long]
  def getByUserName(userName: String): Task[List[InviteNamedRecord]]
  def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]]
  def activatePack(id: Long): Task[Boolean]
  def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int]
}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository {
  import quill.*

  inline given inviteSchemaMeta: SchemaMeta[InviteRecord] = schemaMeta("invites")
  inline given inviteInsertMeta: InsertMeta[InviteRecord] = insertMeta(_.id)
  inline given inviteUpdateMeta: UpdateMeta[InviteRecord] = updateMeta(_.id)

  inline given companySchemaMeta: SchemaMeta[Company] = schemaMeta("companies")
  inline given companyInsertMeta: InsertMeta[Company] = insertMeta(_.id)
  inline given companyUpdateMeta: UpdateMeta[Company] = updateMeta(_.id)

  override def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long] =
    run {
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, userName, companyId, nInvites)))
        .returning(_.id)
    }

  override def getByUserName(userName: String): Task[List[InviteNamedRecord]] =
    run {
      for {
        record <- query[InviteRecord]
          .filter(_.userName == lift(userName))
          .filter(_.nInvites > 0)
          .filter(_.isActive)
        company <- query[Company] if company.id == record.companyId // join condition
      } yield InviteNamedRecord(company.id, company.name, record.nInvites)
    }

  override def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]] =
    run {
      query[InviteRecord]
        .filter(_.companyId == lift(companyId))
        .filter(_.userName == lift(userName))
        .filter(_.isActive)
    }.map(_.headOption)

  override def activatePack(id: Long): Task[Boolean] =
    for {
      current <- run { query[InviteRecord].filter(_.id == lift(id)) }
        .map(_.headOption)
        .someOrFail(new RuntimeException(s"Unable to activate pack $id"))
      result <- run {
        query[InviteRecord]
          .filter(_.id == lift(id))
          .updateValue(lift(current.copy(isActive = true)))
          .returning(_ => true)
      }
    } yield result

  override def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int] =
    for {
      currentRecord <- getInvitePack(userName, companyId)
        .someOrFail(
          new RuntimeException(s"user $userName cannot send invites for company $companyId")
        )
      nInvitesMarked <- ZIO.succeed(Math.min(nInvites, currentRecord.nInvites))
      _ <- run {
        query[InviteRecord]
          .filter(_.id == lift(currentRecord.id))
          .updateValue(lift(currentRecord.copy(nInvites = currentRecord.nInvites - nInvitesMarked)))
          .returning(r => r)
      }
    } yield nInvitesMarked
}

object InviteRepositoryLive {
  val layer: URLayer[Quill.Postgres[SnakeCase], InviteRepository] = ZLayer {
    ZIO.serviceWith[Quill.Postgres[SnakeCase]](new InviteRepositoryLive(_))
  }
}

object InviteRepositoryDemo extends ZIOAppDefault {
  override def run: Task[Unit] = {
    val program = for {
      repo    <- ZIO.service[InviteRepository]
      records <- repo.getByUserName("a@a.com")
      _       <- Console.printLine(s"Records: $records")
    } yield ()

    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}
