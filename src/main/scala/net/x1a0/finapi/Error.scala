package net.x1a0.finapi

import com.twitter.finagle.http.Status
import org.jboss.netty.handler.codec.http.HttpResponseStatus

trait Error {
  def status: HttpResponseStatus
  def message: String

  def toJsonMap = Map(
    "status"  -> status.getCode,
    "message" -> message
  )
}

case class ApiError(
  status: HttpResponseStatus,
  message: String
) extends Error

object Error {
  val NotImplemented   = ApiError(Status.NotImplemented, "not implemented")
  val ResourceNotFound = ApiError(Status.NotFound, "resource not found")
  val ResourceExists   = ApiError(Status.Conflict, "resource exists")
}
