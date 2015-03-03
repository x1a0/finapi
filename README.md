# FinAPI

A [JSON API](http://jsonapi.org/) Implementation for Scala, powered by [Finagle](https://twitter.github.io/finagle/).

[![Build Status](https://api.travis-ci.org/x1a0/finapi.svg?branch=master)](https://travis-ci.org/x1a0/finapi)

## How to use

Add this project as dependency with SBT, in `project/Build.scala`:

```scala
import sbt._

object MyBuild extends Build {
  lazy val root = Project("root", file(".")) dependsOn(finapi)
  lazy val finapi = RootProject(uri("git://github.com/x1a0/finapi.git"))
}
```

Define a `finapi.Resource`:

```scala
import net.x1a0.finapi.Resource

case class Article(
  id: String,
  title: String
) extends Resource
```

Then you can create a useless API:

```scala
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Filter}
import com.twitter.util.Await
import net.x1a0.finapi.Api
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

object Server extends App {
  val nettyToFinagle =
    Filter.mk[HttpRequest, HttpResponse, Request, Response] { (req, service) =>
    service(Request(req)) map { _.httpResponse }
  }

  val api = new Api[Article]("v1")
  val server = Http.serve(":8080", nettyToFinagle andThen api)
  Await.ready(server)
}
```

This generates endpoints:

    GET     /v1/articles
    GET     /v1/articles/<id>
    POST    /v1/articles
    PUT     /v1/articles/<id>
    DELETE  /v1/articles/<id>
    DELETE  /v1/articles?foo=1&bar=2

To make a more useful API, extend `finapi.Api` and override corresponding methods:

```scala
class ArticleApi extends Api[Article] {

  override def list(implicit req: Request): Future[Either[Error, Seq[Article]]] = ???

  override def one(id: String)(implicit req: Request): Future[Either[Error, Option[Article]]] = ???

  override def create(data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, (String, Option[Article])]] = ???

  override def update(id: String, data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, Option[Article]]] = ???

  override def delete(id: String)(implicit req: Request): Future[Either[Error, Unit]] = ???
  
  override def delete()(implicit req: Request): Future[Either[Error, Unit]] = ???
}
```

Check tests to see more examples.

## Tests

    sbt test
