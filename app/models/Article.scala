package models

import java.time.{ Instant, LocalDateTime, ZoneId, ZoneOffset }

import play.api.data._
import play.api.data.Forms.{ text, longNumber, mapping, nonEmptyText, optional }
import play.api.data.validation.Constraints.pattern

case class Article(
  id: Option[String],
  title: String,
  content: String,
  publisher: String,
  creationDate: Option[LocalDateTime],
  updateDate: Option[LocalDateTime])

// Turn off your mind, relax, and float downstream
// It is not dying...
object Article {
  import play.api.libs.json._

  implicit object ArticleWrites extends OWrites[Article] {
    def writes(article: Article): JsObject = Json.obj(
      "_id" -> article.id,
      "title" -> article.title,
      "content" -> article.content,
      "publisher" -> article.publisher,
      "creationDate" -> article.creationDate.fold(-1L)(_.toInstant(ZoneOffset.UTC).toEpochMilli),
      "updateDate" -> article.updateDate.fold(-1L)(_.toInstant(ZoneOffset.UTC).toEpochMilli))
  }

  implicit object ArticleReads extends Reads[Article] {
    def reads(json: JsValue): JsResult[Article] = json match {
      case obj: JsObject => try {
        val id = (obj \ "_id").asOpt[String]
        val title = (obj \ "title").as[String]
        val content = (obj \ "content").as[String]
        val publisher = (obj \ "publisher").as[String]
        val creationDate = (obj \ "creationDate").asOpt[Long]
        val updateDate = (obj \ "updateDate").asOpt[Long]

        JsSuccess(Article(id, title, content, publisher,
          creationDate.map(Instant.ofEpochMilli(_)).map(LocalDateTime.ofInstant(_, ZoneId.systemDefault)),
          updateDate.map(Instant.ofEpochMilli(_)).map(LocalDateTime.ofInstant(_, ZoneId.systemDefault))))
        
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }

  val form = Form(
    mapping(
      "id" -> optional(text verifying pattern(
        """[a-fA-F0-9]{24}""".r, error = "error.objectId")),
      "title" -> nonEmptyText,
      "content" -> text,
      "publisher" -> nonEmptyText,
      "creationDate" -> optional(longNumber),
      "updateDate" -> optional(longNumber)) {
      (id, title, content, publisher, creationDate, updateDate) =>
      Article(
        id,
        title,
        content,
        publisher,
        creationDate.map(Instant.ofEpochMilli(_)).map(LocalDateTime.ofInstant(_, ZoneId.systemDefault)),
        updateDate.map(Instant.ofEpochMilli(_)).map(LocalDateTime.ofInstant(_, ZoneId.systemDefault)))
    } { article =>
      Some(
        (article.id,
          article.title,
          article.content,
          article.publisher,
          article.creationDate.map(_.toInstant(ZoneOffset.UTC).toEpochMilli),
          article.updateDate.map(_.toInstant(ZoneOffset.UTC).toEpochMilli)))
    })
}
