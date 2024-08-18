package com.rite.services

import com.rite.config.{Configs, SummaryConfig}
import com.rite.domain.data.{Review, ReviewSummary}
import com.rite.http.requests.CreateReviewRequest
import com.rite.repositories.ReviewRepository
import zio.*

trait ReviewService {
  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(companyId: Long): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getSummary(companyId: Long): Task[Option[ReviewSummary]]
  def makeSummary(companyId: Long): Task[Option[ReviewSummary]]
}

class ReviewServiceLive private (
    repo: ReviewRepository,
    openaiService: OpenAIService,
    config: SummaryConfig
) extends ReviewService {

  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(request.toReview(-1L, userId))

  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)

  override def getByCompanyId(companyId: Long): Task[List[Review]] =
    repo.getByCompanyId(companyId)

  override def getByUserId(userId: Long): Task[List[Review]] =
    repo.getByUserId(userId)

  override def getSummary(companyId: Long): Task[Option[ReviewSummary]] =
    repo.getSummary(companyId)

  override def makeSummary(companyId: Long): Task[Option[ReviewSummary]] =
    getByCompanyId(companyId)
      .flatMap(Random.shuffle)
      .map(_.take(config.nSelected))
      .flatMap { reviews =>
        val currentSummary: Task[Option[String]] =
          if (reviews.size < config.minReviews)
            ZIO.some(s"Need to have at least ${config.minReviews} reviews to generate a summary")
          else
            buildPrompt(reviews).flatMap(openaiService.getCompletion)
        currentSummary.flatMap {
          case None          => ZIO.none
          case Some(summary) => repo.insertSummary(companyId, summary).map(Some(_))
        }
      }

  private def buildPrompt(reviews: List[Review]): Task[String] = ZIO.succeed {
    val reviewsContent = reviews.zipWithIndex.map { case (r, index) =>
      s"""
         |Review ${index + 1}:
         |  Management: ${r.management} stars / 5
         |  Culture: ${r.culture} stars / 5
         |  Salary: ${r.salary} stars / 5
         |  Benefits: ${r.benefits} stars / 5
         |  Net promoter score: ${r.wouldRecommend} stars / 5
         |  Content: "$r"
         |""".stripMargin
        .mkString("\n")
    }

    "You have the following reviews about a company:" + reviewsContent +
      "Make a summary of all these reviews in at most one paragraph."
  }
}

object ReviewServiceLive {
  private type R = OpenAIService & ReviewRepository

  val layer: URLayer[SummaryConfig & R, ReviewService] = ZLayer {
    for {
      repo          <- ZIO.service[ReviewRepository]
      openaiService <- ZIO.service[OpenAIService]
      config        <- ZIO.service[SummaryConfig]
    } yield new ReviewServiceLive(repo, openaiService, config)
  }

  val configuredLayer: RLayer[R, ReviewService] =
    Configs.makeConfigLayer[SummaryConfig]("rite.summaries") >>> layer
}
