package controllers

import javax.inject.Inject

import org.joda.time.DateTime

import scala.concurrent.Future

import play.api.Logger

import play.api.mvc.{ AbstractController, ControllerComponents, Request }
import play.api.libs.json.{ Json, JsObject, JsString }

import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.ReadFile

import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import models.Article, Article._

class Articles @Inject() (
  components: ControllerComponents,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val materializer: akka.stream.Materializer
) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents {

  import java.util.UUID
  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  implicit def ec = components.executionContext

  // get the collection 'articles'
  def collection = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("articles"))

  // a GridFS store named 'attachments'
  private def gridFS: Future[MongoController.JsGridFS] = for {
    fs <- reactiveMongoApi.asyncGridFS
    _ <- fs.ensureIndex().map { index =>
      // let's build an index on our gridfs chunks collection if none
      Logger(s"Checked index, result is $index")
    }
  } yield fs

  // list all articles and sort them
  val index = Action.async { implicit request =>
    // get a sort document (see getSort method for more information)
    val sort: JsObject = getSort(request).getOrElse(Json.obj())

    val activeSort = request.queryString.get("sort").
      flatMap(_.headOption).getOrElse("none")

    // the cursor of documents
    val found = collection.map(
      _.find(Json.obj(), Option.empty[JsObject]).sort(sort).cursor[Article]())

    // build (asynchronously) a list containing all the articles
    found.flatMap(_.collect[List](-1, Cursor.FailOnError[List[Article]]())).
      map { articles =>
        Ok(views.html.articles(articles, activeSort))
      }.recover {
        case e =>
          e.printStackTrace()
          BadRequest(e.getMessage())
      }
  }

  def showCreationForm = Action { implicit request =>
    implicit val messages = messagesApi.preferred(request)

    Ok(views.html.editArticle(None, Article.form, None))
  }

  def showEditForm(id: String) = Action.async { implicit request =>
    // get the documents having this id (there will be 0 or 1 result)
    def futureArticle = collection.flatMap(
      _.find(Json.obj("_id" -> id), Option.empty[JsObject]).one[Article])

    // ... so we get optionally the matching article, if any
    // let's use for-comprehensions to compose futures
    for {
      // get a future option of article
      maybeArticle <- futureArticle
      // if there is some article, return a future of result
      // with the article and its attachments
      fs <- gridFS
      result <- maybeArticle.map { article =>
        // search for the matching attachments
        // find(...).toList returns a future list of documents
        // (here, a future list of ReadFileEntry)
        fs.find[JsObject, JsString](
          Json.obj("article" -> article.id.get)).
          collect[List](-1, Cursor.FailOnError[List[JSONReadFile]]()).
          map { files =>

          @inline def filesWithId = files.map { file => file.id -> file }
          implicit val messages = messagesApi.preferred(request)
          
          Ok(views.html.editArticle(Some(id),
            Article.form.fill(article), Some(filesWithId)))
        }
      }.getOrElse(Future.successful(NotFound))
    } yield result
  }

  def create = Action.async { implicit request =>
    implicit val messages = messagesApi.preferred(request)

    Article.form.bindFromRequest.fold(
      errors => Future.successful(
        Ok(views.html.editArticle(None, errors, None))),

      // if no error, then insert the article into the 'articles' collection
      article => collection.flatMap(_.insert.one(article.copy(
        id = article.id.orElse(Some(UUID.randomUUID().toString)),
        creationDate = Some(new DateTime()),
        updateDate = Some(new DateTime()))
      )).map(_ => Redirect(routes.Articles.index))
    )
  }

  def edit(id: String) = Action.async { implicit request =>
    implicit val messages = messagesApi.preferred(request)
    import reactivemongo.bson.BSONDateTime

    Article.form.bindFromRequest.fold(
      errors => Future.successful(
        Ok(views.html.editArticle(Some(id), errors, None))),

      article => {
        // create a modifier document, ie a document that contains the update operations to run onto the documents matching the query
        val modifier = Json.obj(
          // this modifier will set the fields
          // 'updateDate', 'title', 'content', and 'publisher'
          "$set" -> Json.obj(
            "updateDate" -> BSONDateTime(new DateTime().getMillis),
            "title" -> article.title,
            "content" -> article.content,
            "publisher" -> article.publisher))

        // ok, let's do the update
        collection.flatMap(_.update.one(Json.obj("_id" -> id), modifier).
          map { _ => Redirect(routes.Articles.index) })
      })
  }

  def delete(id: String) = Action.async {
    // let's collect all the attachments matching that match the article to delete
    (for {
      fs <- gridFS
      files <- fs.find[JsObject, JsString](Json.obj("article" -> id)).
      collect[List](-1, Cursor.FailOnError[List[JSONReadFile]]())
      _ <- {
        // for each attachment, delete their chunks and then their file entry
        def deletions = files.map(fs.remove(_))

        Future.sequence(deletions)
      }
      coll <- collection
      _ <- {
        // now, the last operation: remove the article
        coll.delete.one(Json.obj("_id" -> id))
      }
    } yield Ok).recover { case _ => InternalServerError }
  }

  // save the uploaded file as an attachment of the article with the given id
  def saveAttachment(id: String) = {
    val gfs = gridFS

    Action.async(gridFSBodyParser(gfs)) { request =>
      val file = request.body.files.head.ref

      // when the upload is complete, we add the article id to the file entry
      // (in order to find the attachments of the article)

      (for {
        fileColl <- reactiveMongoApi.database.map(
          _.collection[JSONCollection]("fs.files"))

        _ <- fileColl.update.one(
          Json.obj("_id" -> file.id),
          Json.obj(f"$$set" -> Json.obj("article" -> id)))
      } yield Redirect(routes.Articles.showEditForm(id))).recover {
        case e => InternalServerError(e.getMessage())
      }
    }
  }

  def getAttachment(id: String) = Action.async { request =>
    gridFS.flatMap { fs =>
      // find the matching attachment, if any, and streams it to the client
      val file = fs.find[JsObject, JsString](Json.obj("_id" -> id))

      request.getQueryString("inline") match {
        case Some("true") =>
          serve[JsString, JSONReadFile](fs)(file, CONTENT_DISPOSITION_INLINE)

        case _            => serve[JsString, JSONReadFile](fs)(file)
      }
    }
  }

  def removeAttachment(id: String) = Action.async {
    gridFS.flatMap(_.remove(Json toJson id).map(_ => Ok).
      recover { case _ => InternalServerError })
  }

  private def getSort(request: Request[_]): Option[JsObject] =
    request.queryString.get("sort").map { fields =>
      val sortBy = for {
        order <- fields.map { field =>
          if (field.startsWith("-"))
            field.drop(1) -> -1
          else field -> 1
        }
        if order._1 == "title" || order._1 == "publisher" || order._1 == "creationDate" || order._1 == "updateDate"
      } yield order._1 -> implicitly[Json.JsValueWrapper](Json.toJson(order._2))

      Json.obj(sortBy: _*)
    }

}
