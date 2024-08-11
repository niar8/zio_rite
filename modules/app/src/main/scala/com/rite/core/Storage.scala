package com.rite.core

import org.scalajs.dom
import zio.json.*

object Storage {
  def set[A: JsonEncoder](key: String, value: A): Unit =
    dom.window.localStorage.setItem(key, value.toJson)

  def get[A](key: String)(using decoder: JsonDecoder[A]): Option[A] =
    Option(dom.window.localStorage.getItem(key))
      .filter(_.nonEmpty)
      .flatMap(decoder.decodeJson(_).toOption)

  def remove(key: String): Unit =
    dom.window.localStorage.removeItem(key)
}
