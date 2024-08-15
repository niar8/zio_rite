package com.rite.services

import com.rite.domain.data.*
import com.rite.repositories.*
import zio.*

trait InviteService {
  def addInvitePack(userName: String, companyId: Long): Task[Long]
  def sendInvites(userName: String, companyId: Long, receivers: List[String]): Task[Int]
  def getByUserName(userName: String): Task[List[InviteNamedRecord]]
}

class InviteServiceLive private (
    inviteRepo: InviteRepository,
    companyRepo: CompanyRepository
) extends InviteService {
  // invariant: only one pack per user per company
  override def addInvitePack(userName: String, companyId: Long): Task[Long] =
    for {
      company <- companyRepo
        .getById(companyId)
        .someOrFail(
          new RuntimeException(s"Cannot invite to review: company $companyId doesn't exist")
        )
      currentPack <- inviteRepo.getInvitePack(userName, companyId)
      newPackId <- currentPack match {
        case None =>
          inviteRepo.addInvitePack(userName, companyId, 200) // TODO configure this
        case Some(_) =>
          ZIO.fail(new RuntimeException("You already have an active pack for this company"))
      }
      // TODO Remove after implementing the payment process
      _ <- inviteRepo.activatePack(newPackId)
    } yield newPackId

  override def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[RuntimeFlags] =
    ZIO.fail(new RuntimeException("Not implemented yet"))

  override def getByUserName(userName: String): Task[List[InviteNamedRecord]] =
    inviteRepo.getByUserName(userName)
}

object InviteServiceLive {
  val layer: URLayer[CompanyRepository & InviteRepository, InviteService] = ZLayer {
    for {
      inviteRepo  <- ZIO.service[InviteRepository]
      companyRepo <- ZIO.service[CompanyRepository]
    } yield new InviteServiceLive(inviteRepo, companyRepo)
  }
}
