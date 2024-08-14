package com.rite.services

import com.rite.domain.data.*
import zio.*

trait InviteService {
  def addInvitePack(userName: String, companyId: Long): Task[Long]
  def sendInvites(userName: String, companyId: Long, receivers: List[String]): Task[Int]
  def getByUserId(userName: String): Task[List[InviteNamedRecord]]
}

class InviteServiceLive private extends InviteService {
  override def addInvitePack(userName: String, companyId: Long): Task[Long] =
    ZIO.fail(new RuntimeException("Not implemented yet"))

  override def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[RuntimeFlags] =
    ZIO.fail(new RuntimeException("Not implemented yet"))

  override def getByUserId(userName: String): Task[List[InviteNamedRecord]] =
    ZIO.fail(new RuntimeException("Not implemented yet"))
}

object InviteServiceLive {
  val layer: ULayer[InviteServiceLive] = ZLayer {
    ZIO.succeed(new InviteServiceLive())
  }
}
