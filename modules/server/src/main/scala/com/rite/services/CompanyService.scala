package com.rite.services

import zio.*

import collection.mutable
import com.rite.domain.data.*
import com.rite.http.requests.CreateCompanyRequest
import com.rite.repositories.CompanyRepository

trait CompanyService {
  def create(req: CreateCompanyRequest): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getAll: Task[List[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {
  override def create(req: CreateCompanyRequest): Task[Company] =
    repo.create(req.toCompany(-1L))

  override def getById(id: Long): Task[Option[Company]] =
    repo.getById(id)

  override def getAll: Task[List[Company]] =
    repo.getAll

  override def getBySlug(slug: String): Task[Option[Company]] =
    repo.getBySlug(slug)
}

object CompanyServiceLive {
  val layer: URLayer[CompanyRepository, CompanyService] = ZLayer {
    ZIO.serviceWith[CompanyRepository](new CompanyServiceLive(_))
  }
}
