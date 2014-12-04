package net.x1a0.finapi

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.Service
import com.twitter.util.Future
import org.atteo.evo.inflector.English
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.json4s.{JValue, JObject}
import scala.util.{Try, Success, Failure}

class Api[R <: Resource, REQ <: Request](version: String)(implicit resourceTag: reflect.ClassTag[R])
    extends Service[REQ, Response] {

  /** override to use custom name in JSON and URLs */
  val resourceName: String =
    English.plural(resourceTag.runtimeClass.getSimpleName).toLowerCase

  private[this] val prefix = Root / version / resourceName

  private[this] implicit val formats = DefaultFormats

  def apply(request: REQ): Future[Response] = {

    /** GET     /v1/articles
      * GET     /v1/articles/<id>
      * POST    /v1/articles
      * PUT     /v1/articles/<id>
      * DELETE  /v1/articles/<id>

      * @TODO
      * OPTIONS /v1/articles
      * OPTIONS /v1/articles/<id>
      * GET     /v1/articles/<id>,<id>,...,<id>
      */

    implicit val req = request

    (request.method, Path(request.path)) match {
      case Get -> `prefix` =>
        list map {
          case Right(resources) =>
            val res = request.response
            res.contentType = "application/vnd.api+json"
            res.contentString = write(Map(resourceName -> resources))
            res

          case Left(error) =>
            errorResponse(error)
        }

      case Get -> `prefix` / id =>
        one(id) map {
          case Right(Some(resource)) =>
            val res = request.response
            res.contentType = "application/vnd.api+json"
            res.contentString = write(Map(resourceName -> resource))
            res

          case Right(None) =>
            val res = request.response
            res.status = NotFound
            res

          case Left(error) =>
            errorResponse(error)
        }

      case Post -> `prefix` =>
        parseBody[JObject](request.contentString) fold (
          error => Future.value(errorResponse(error)),
          json => create(json.map(_.extract[Map[String, Any]])) map {
            case Right((id, Some(resource))) =>
              val res = request.response
              res.status = Created
              res.contentType = "application/vnd.api+json"
              res.headerMap += ("Location" -> (prefix / id).toString)
              res.contentString = write(Map(resourceName -> resource))
              res

            case Right((id, None)) =>
              val res = request.response
              res.status = Created
              res.headerMap += ("Location" -> s"/$resourceName/$id")
              res

            case Left(error) =>
              errorResponse(error)
          }
        )

      case Put -> `prefix` / id =>
        parseBody[JObject](request.contentString) fold (
          error => Future.value(errorResponse(error)),
          json => update(id, json.map(_.extract[Map[String, Any]])) map {
            case Right(Some(resource)) =>
              val res = request.response
              res.status = Ok
              res.contentType = "application/vnd.api+json"
              res.contentString = write(Map(resourceName -> resource))
              res

            case Right(None) =>
              val res = request.response
              res.status = NoContent
              res

            case Left(error) =>
              errorResponse(error)
          }
        )

      case Delete -> `prefix` / id =>
        delete(id) map {
          case Right(_) =>
            val res = request.response
            res.status = NoContent
            res

          case Left(error) =>
            errorResponse(error)
        }

      case _ =>
        Future.value {
          errorResponse(ApiError(NotFound, "unrecognized url"))
        }
    }
  }

  private[this] def errorResponse(error: Error)(implicit req: REQ): Response = {
    val res = req.response
    res.status = error.status
    res.contentType = "application/vnd.api+json"
    res.contentString = write(Map("error" -> error.toJsonMap))
    res
  }

  private[this] def parseBody[T <: JValue](body: String)(implicit tag: reflect.ClassTag[T]): Either[Error, Option[T]] = {
    if (body == "") Right(None)
    else {
      (Try {
        parse(body) \ resourceName
      } map {
        case json: T => Right(Some(json))
        case _ => Left(ApiError(BadRequest, "bad json format"))
      } recover {
        case ex: Throwable => Left(ApiError(BadRequest, ex.getMessage))
      }).get
    }
  }

  def list(implicit req: REQ): Future[Either[Error, Seq[R]]]
    = Future.value(Left(Error.NotImplemented))

  def one(id: String)(implicit req: REQ): Future[Either[Error, Option[R]]]
    = Future.value(Left(Error.NotImplemented))

  /** If this method returns the primary resource created, it will be
    * included in the response. Otherwise response content will be empty
    * and the client SHOULD treat the transmitted resource as accepted
    * without modification.
    */
  def create(data: Option[Map[String, Any]])(implicit req: REQ): Future[Either[Error, (String, Option[R])]]
    = Future.value(Left(Error.NotImplemented))

  /** If this method returns the primary resource updated, it will be
    * included in the response. Otherwise response content will be empty
    * and the client SHOULD treat the transmitted resource as accepted
    * without modification.
    */
  def update(id: String, data: Option[Map[String, Any]])(implicit req: REQ): Future[Either[Error, Option[R]]]
    = Future.value(Left(Error.NotImplemented))

  def delete(id: String)(implicit req: REQ): Future[Either[Error, Unit]]
    = Future.value(Left(Error.NotImplemented))
}
