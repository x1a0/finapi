package net.x1a0.finapi

import com.twitter.finagle.http.Method._
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status._
import com.twitter.util.{Await, Future}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.scalatest._
import org.scalatest.mock.MockitoSugar

sealed case class Article(
  id: String,
  title: String
) extends Resource

class ApiSpec extends FreeSpec
    with Matchers
    with MockitoSugar {

  implicit val formats = DefaultFormats

  class ArticleApi extends Api[Article, Request]("v1") {
    self: ArticleStore =>

    override def list(implicit req: Request): Future[Either[Error, Seq[Article]]] = {
      Future.value(Right(store.values.toSeq.sortBy(_.id)))
    }

    override def one(id: String)(implicit req: Request): Future[Either[Error, Option[Article]]] = {
      Future.value(Right(store.get(id)))
    }

    override def create(data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, (String, Option[Article])]] = {
      // ugly hack: always create article "1"
      store.get("1").fold[Future[Either[Error, (String, Option[Article])]]]({
        val article = Article("1", title = data.get("title").asInstanceOf[String])
        store.update("1", article)
        Future.value(Right("1", Some(article)))
      })(
        article => Future.value(Left(Error.ResourceExists))
      )
    }

    override def update(id: String, data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, Option[Article]]] = {
      store.get(id).fold[Future[Either[Error, Option[Article]]]](
        Future.value(Left(Error.ResourceNotFound))
      )(
        // ugly hack: when updating article 2, return the updated article in response
        article => {
          store.update(id, article.copy(title = data.get("title").asInstanceOf[String]))
          id match {
            case "2" => Future.value(Right(store.get(id)))
            case _   => Future.value(Right(None))
          }
        }
      )
    }

    override def delete(id: String)(implicit req: Request): Future[Either[Error, Unit]] = {
      store.get(id).fold[Future[Either[Error, Unit]]](
        Future.value(Left(Error.ResourceNotFound))
      )(
        _ => Future.value(Right(Unit))
      )
    }
  }

  trait ArticleStore {
    val store = collection.mutable.Map.empty[String, Article]
  }

  "GETs collections" - {
    "when resources found" - {
      val api = new ArticleApi with ArticleStore {
        store.update("1", Article("1", title = "Foo"))
        store.update("2", Article("2", title = "Bar"))
      }
      val req = Request("/v1/articles")
      val res = Await.result(api(req))

      "SHOULD return 200 OK" in {
        res.status shouldEqual Ok
      }

      "SHOULD set Content-Type to \"application/vnd.api+json\"" in {
        res.contentType shouldEqual Some("application/vnd.api+json")
      }

      "SHOULD return a sequence of resource" in {
        parse(res.contentString).extract[Map[String, Any]] shouldEqual Map(
          "articles" -> Seq(
            Map("id" -> "1", "title" -> "Foo"),
            Map("id" -> "2", "title" -> "Bar")
          )
        )
      }
    }

    "when resources not found" - {
      val api = new ArticleApi with ArticleStore
      val req = Request("/v1/articles")
      val res = Await.result(api(req))

      "SHOULD return 200 OK" in {
        res.status shouldEqual Ok
      }

      "SHOULD set Content-Type to \"application/vnd.api+json\"" in {
        res.contentType shouldEqual Some("application/vnd.api+json")
      }

      "SHOULD return an empty sequence of resource" in {
        parse(res.contentString).extract[Map[String, Any]] shouldEqual Map(
          "articles" -> Seq()
        )
      }
    }
  }

  "GETs single resource" - {
    val api = new ArticleApi with ArticleStore {
      store.update("1", Article("1", title = "Foo"))
    }

    "when resource found" - {
      val req = Request("/v1/articles/1")
      val res = Await.result(api(req))

      "SHOULD return 200 OK" in {
        res.status shouldEqual Ok
      }

      "SHOULD set Content-Type to \"application/vnd.api+json\"" in {
        res.contentType shouldEqual Some("application/vnd.api+json")
      }

      "SHOULD return the resource" in {
        parse(res.contentString).extract[Map[String, Any]] shouldEqual Map(
          "articles" -> Map("id" -> "1", "title" -> "Foo")
        )
      }
    }

    "when resource not found" - {
      val req = Request("/v1/articles/2")
      val res = Await.result(api(req))

      "SHOULD return 404 Not Found" in {
        res.status shouldEqual NotFound
      }
    }
  }

  "POSTs single resource" - {
    val api = new ArticleApi with ArticleStore

    "when resource is created" - {
      val req = Request(Post, "/v1/articles")
      req.contentString = """{"articles":{"title":"Foo"}}"""
      val res = Await.result(api(req))

      "MUST return 201 Created" in {
        res.status shouldEqual Created
      }

      "MUST include a Location header matches href value of the resource" in {
        res.headerMap("Location") shouldEqual "/v1/articles/1"
      }

      "SHOULD include a Content-Type header set to \"application/vnd.api+json\"" in {
        res.contentType shouldEqual Some("application/vnd.api+json")
      }

      "SHOULD include a resource that contains the primary resource created" in {
        parse(res.contentString).extract[Map[String, Any]] shouldEqual Map(
          "articles" -> Map("id" -> "1", "title" -> "Foo")
        )
      }
    }

    "when failed to create resource" - {
      val req = Request(Post, "/v1/articles")
      req.contentString = """{"articles":{"title":"Foo"}}"""
      val res = Await.result(api(req))

      "SHOULD return 409 Confilict if the resource already exists" in {
        res.status shouldEqual Conflict
      }
    }
  }

  "PUTs individual resource" - {
    val api = new ArticleApi with ArticleStore {
      store.update("1", Article("1", title = "Foo"))
      store.update("2", Article("2", title = "Bar"))
    }

    "when updating is successful and client's current attributes remain up to date" - {
      val req = Request(Put, "/v1/articles/1")
      req.contentString = """{"articles":{"title":"FooBar"}}"""
      val res = Await.result(api(req))

      "SHOULD return 204 No Content" in {
        res.status shouldEqual NoContent
      }
    }

    "when server accepts an update but also changes the resource in other ways than those specified by the request" - {
      val req = Request(Put, "/v1/articles/2")
      req.contentString = """{"articles":{"title":"BarBaz"}}"""
      val res = Await.result(api(req))

      "SHOULD return 200 OK" in {
        res.status shouldEqual Ok
      }

      "MUST include a representation of the updated resource as if a GET request was made to the request URL" in {
        parse(res.contentString).extract[Map[String, Map[String, Any]]] shouldEqual Map(
          "articles" -> Map("id" -> "2", "title" -> "BarBaz")
        )
      }
    }

    "when specified resource does not exist" - {
      val req = Request(Put, "/v1/articles/3")
      req.contentString = """{"articles":{"title":"Bar"}}"""
      val res = Await.result(api(req))

      "SHOULD return 404 Not Found" in {
        res.status shouldEqual NotFound
      }
    }
  }

  "DELETEs individual resource" - {
    val api = new ArticleApi with ArticleStore {
      store.update("1", Article("1", title = "Foo"))
    }

    "when specified resource is successfully deleted" - {
      val req = Request(Delete, "/v1/articles/1")
      val res = Await.result(api(req))

      "MUST return 204 No Content" in {
        res.status shouldEqual NoContent
      }
    }

    "when specified resource does not exist" - {
      val req = Request(Delete, "/v1/articles/2")
      val res = Await.result(api(req))

      "SHOULD return 404 Not Found" in {
        res.status shouldEqual NotFound
      }
    }
  }
}
