package services

import play.api._
import play.api.Play.current
import models._
import java.nio.charset.StandardCharsets
import java.util.Date
import java.net.URI
import play.api.libs.oauth._
import scala.util.Try
import play.api.mvc.RequestHeader
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequestHolder
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future
import org.apache.http.HttpStatus
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import play.api.libs.json.Reads
import play.api.libs.ws.WSRequest

trait JiraApiService {

  /**
   * Returns all projects which are visible for the currently logged in user. If no user is logged in, it returns the list of projects that are visible when using anonymous access.
   */
  def getAllProjects(expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraProject]]

  /**
   * Returns all versions for the specified project. Results are paginated.
   */
  def getProjectVersions(projectIdOrKey: String, startAt: Integer = 0, maxResults: Integer = 50,
    orderBy: String = "", expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraVersion]]

  /**
   * Contains a full representation of a the specified project's versions.
   */
  def getVersions(projectIdOrKey: String, expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraVersion]]

  /**
   * Searches for issues using JQL.
   */
  def findIssues(jql: String, startAt: Integer = 0, maxResults: Integer = 50,
    validateQuery: Boolean = true, fields: String = "*navigatable", expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraIssue]]
}

sealed trait JiraAuthentication
case class BasicAuthentication(username: String, password: String) extends JiraAuthentication
case class OAuthAuthentication(token: String) extends JiraAuthentication

case class JiraConfiguration(baseUrl: String)

trait JiraApiServiceImpl extends JiraApiService {

  import services.JiraWSHelper._

  val allProjectsUrl = "/rest/api/2/project?%s"
  val projectVersionsUrl = "/rest/api/2/project/%s/version?%d&%d&%s&%s"
  val versionsUrl = "/rest/api/2/project/%s/versions?%s"
  val findIssuesUrl = "/rest/api/2/search?%s&%d&%d&%b&%s&%s"

  val ws: WSClient
  val config: JiraConfiguration

  def getAllProjects(expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraProject]] = {
    val url = allProjectsUrl.format(expand)
    getList[JiraProject](url)
  }

  def getProjectVersions(projectIdOrKey: String, startAt: Integer = 0, maxResults: Integer = 50, orderBy: String = "", expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext) = {
    val url = projectVersionsUrl.format(projectIdOrKey, startAt, maxResults, orderBy, expand)
    getList[JiraVersion](url)
  }

  def getVersions(projectIdOrKey: String, expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraVersion]] = {
    val url = versionsUrl.format(projectIdOrKey, expand)
    getList[JiraVersion](url)
  }

  def findIssues(jql: String, startAt: Integer = 0, maxResults: Integer = 50,
    validateQuery: Boolean = true, fields: String = "*navigatable", expand: String = "")(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Seq[JiraIssue]] = {
    val url = findIssuesUrl.format(jql, startAt, maxResults, validateQuery, fields, expand)
    getList[JiraIssue](url)
  }

  def getList[T](relUrl: String)(implicit auth: JiraAuthentication, executionContext: ExecutionContext, reads: Reads[T]): Future[Seq[T]] = {
    val url = config.baseUrl + relUrl
    ws.url(url).withJiraCredentials.get.map { resp =>
      Logger.debug(s"Called api url:$relUrl => Status=${resp.status}:${resp.statusText}, Headers:${resp.allHeaders}")
      resp.status match {
        case HttpStatus.SC_OK => Json.fromJson[Seq[T]](resp.json).asOpt.getOrElse(Nil)
        case _ => Nil
      }
    }
  }

  def getOption[T](relUrl: String)(implicit auth: JiraAuthentication, executionContext: ExecutionContext, reads: Reads[T]): Future[Option[T]] = {
    val url = config.baseUrl + relUrl
    ws.url(url).withJiraCredentials.get.map { resp =>
      Logger.debug(s"Called api url:$relUrl => Status=${resp.status}:${resp.statusText}, Headers:${resp.allHeaders}")
      resp.status match {
        case HttpStatus.SC_OK => Json.fromJson[T](resp.json).asOpt
        case _ => None
      }
    }
  }
}

object JiraWSHelper {
  implicit class JiraWS(self: WSRequest) {

    def withJiraCredentials(implicit auth: JiraAuthentication): WSRequest = {
      self.withHeaders(headers: _*).withRequestTimeout(10000)
    }

    def headers(implicit auth: JiraAuthentication) = {
      val h1 = ("Content-Type" -> "application/json")
      val h2 = auth match {
        case BasicAuthentication(user, pwd) =>
          val pair = s"$user:$pwd"
          val encPart = new String(Base64.encodeBase64(pair.getBytes("utf-8")), "utf-8")
          val enc = s"Basic $encPart"
          ("Authorization" -> enc)
        case OAuthAuthentication(token) =>
          ("oauth_token" -> token)
      }
      Seq(h1, h2)
    }
  }
}

//object JiraApiServiceImpl extends JiraApiServiceImpl