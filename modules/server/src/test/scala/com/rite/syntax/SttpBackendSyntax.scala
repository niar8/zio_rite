package com.rite.syntax

import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.model.Method
import zio.Task
import zio.json.JsonCodec
import zio.json.EncoderOps

extension [A: JsonCodec](backend: SttpBackend[Task, Any]) {
  private def sendRequest[B: JsonCodec](
      method: Method,
      path: String,
      payload: A,
      maybeToken: Option[String] = None
  ): Task[Option[B]] =
    basicRequest
      .method(method, uri"$path")
      .body(payload.toJson)
      .auth
      .bearer(maybeToken.getOrElse(""))
      .send(backend)
      .map(_.tryParseTo[B])

  def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
    sendRequest(Method.POST, path, payload, None)
  def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
    sendRequest(Method.POST, path, payload, Some(token))
  def postNoResponse(path: String, payload: A): Task[Unit] =
    basicRequest
      .method(Method.POST, uri"$path")
      .body(payload.toJson)
      .send(backend)
      .unit
  def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
    sendRequest(Method.PUT, path, payload, None)
  def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
    sendRequest(Method.PUT, path, payload, Some(token))
  def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
    sendRequest(Method.DELETE, path, payload, None)
  def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
    sendRequest(Method.DELETE, path, payload, Some(token))
}
