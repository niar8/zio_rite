package com.rite.repositories

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

import javax.sql.DataSource

trait Repository[T] {
  def create(item: T): Task[T]
  def getById(id: Long): Task[Option[T]]
  def getAll: Task[List[T]]
  def update(id: Long, op: T => T): Task[T]
  def delete(id: Long): Task[T]
}

object Repository {
  // if something goes wrong, use 'SnakeCase.type'
  def quillLayer: URLayer[DataSource, Quill.Postgres[SnakeCase]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer: TaskLayer[DataSource] =
    Quill.DataSource.fromPrefix("rite.db")

  val dataLayer: TaskLayer[Quill.Postgres[SnakeCase]] =
    dataSourceLayer >>> quillLayer
}
