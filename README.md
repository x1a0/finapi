# Finapi

A [JSON API](http://jsonapi.org/) Implementation for Scala, powered by [Finagle](https://twitter.github.io/finagle/).

## How to use

Define a `finapi.Resource`:

    import net.x1a0.finapi.Resource

    case class Article(
      id: String,
      title: String
    ) extends Resource

Then you can create a useless API:

    import net.x1a0.finapi.Api

    val api = new Api[Article]("v1")

This generates endpoints:

    GET     /v1/articles
    GET     /v1/articles/<id>
    POST    /v1/articles
    PUT     /v1/articles/<id>
    DELETE  /v1/articles/<id>

To make a more useful API, extend `finapi.Api` and override corresponding methods:

    class ArticleApi extends Api[Article] {

      override def list(implicit req: Request): Future[Either[Error, Seq[Article]]] = {
        ...
      }

      override def one(id: String)(implicit req: Request): Future[Either[Error, Option[Article]]] = {
        ...
      }

      override def create(data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, (String, Option[Article])]] = {
        ...
      }

      override def update(id: String, data: Option[Map[String, Any]])(implicit req: Request): Future[Either[Error, Option[Article]]] = {
        ...
      }

      override def delete(id: String)(implicit req: Request): Future[Either[Error, Unit]] = {
        ...
      }
    }

Check tests to see more examples.

## Tests

    sbt test
