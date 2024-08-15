package com.rite.services

import com.rite.config.*
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
    companyRepo: CompanyRepository,
    emailService: EmailService,
    config: InvitePackConfig
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
          inviteRepo.addInvitePack(userName, companyId, config.nInvites)
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
    for {
      company <- companyRepo
        .getById(companyId)
        .someOrFail(new RuntimeException(s"Cannot send invites: company $companyId doesn't exist"))
      nInvitesMarked <- inviteRepo.markInvites(userName, companyId, nInvites = receivers.size)
      _ <- ZIO.foreachParDiscard(receivers.take(nInvitesMarked)) { receiver =>
        emailService.sendReviewInvite(userName, receiver, company)
      }
    } yield 0

  override def getByUserName(userName: String): Task[List[InviteNamedRecord]] =
    inviteRepo.getByUserName(userName)
}

object InviteServiceLive {
  private type R = EmailService & CompanyRepository & InviteRepository

  val layer: URLayer[InvitePackConfig & R, InviteService] = ZLayer {
    for {
      inviteRepo   <- ZIO.service[InviteRepository]
      companyRepo  <- ZIO.service[CompanyRepository]
      emailService <- ZIO.service[EmailService]
      config       <- ZIO.service[InvitePackConfig]
    } yield new InviteServiceLive(inviteRepo, companyRepo, emailService, config)
  }

  val configuredLayer: RLayer[R, InviteService] =
    Configs.makeConfigLayer[InvitePackConfig]("rite.invites") >>> layer
}
