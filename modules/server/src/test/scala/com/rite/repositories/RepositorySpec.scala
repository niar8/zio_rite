package com.rite.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.*

import javax.sql.DataSource

trait RepositorySpec {
  val initScript: String

  val dataSourceLayer: RLayer[Scope, DataSource] = ZLayer {
    for {
      container <- ZIO.acquireRelease(createContainer) { container =>
        ZIO.attempt(container.stop()).ignoreLogged
      }
      dataSource <- createDataSource(container)
    } yield dataSource
  }

  private def createContainer: Task[PostgreSQLContainer[Nothing]] =
    ZIO.attempt {
      val container: PostgreSQLContainer[Nothing] =
        PostgreSQLContainer("postgres").withInitScript(initScript)
      container.start()
      container
    }

  private def createDataSource(container: PostgreSQLContainer[Nothing]): Task[PGSimpleDataSource] =
    ZIO.attempt {
      val dataSource = new PGSimpleDataSource()
      dataSource.setUrl(container.getJdbcUrl)
      dataSource.setUser(container.getUsername)
      dataSource.setPassword(container.getPassword)
      dataSource
    }
}
