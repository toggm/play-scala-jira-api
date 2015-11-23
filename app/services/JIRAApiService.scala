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
import play.api.mvc.Request
import utils.OAuthUtil
import net.oauth.client.OAuthClient
import net.oauth.client.httpclient4.HttpClient4
import play.api.libs.json.JsValue
import scala.util.Failure
import scala.util.Success
import scala.util.Success
import org.apache.http.HttpException
import java.io.IOException
import scala.util.Failure

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
case class OAuthAuthentication(consumerKey:String, privateKey:String, token: String) extends JiraAuthentication

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
    Logger.debug(s"getAllProjects(expand:$expand, url:$url")
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
    Logger.debug(s"getList(url:$url")
    JiraWSHelper.call(config, url, ws).flatMap { _ match {
      case Success(json) => 
        Logger.debug(s"getList:Success -> $json")
        Json.fromJson[Seq[T]](json).asOpt.map(j => Future.successful(j)).getOrElse(Future.failed(new RuntimeException(s"Could not parse $json")))
      case Failure(e) =>
        Logger.debug(s"getList:Failure -> $e")
        Future.failed(e)
    }}    
  }

  def getOption[T](relUrl: String)(implicit auth: JiraAuthentication, executionContext: ExecutionContext, reads: Reads[T]): Future[Option[T]] = {
    val url = config.baseUrl + relUrl
    Logger.debug(s"getOption(url:$url")
    JiraWSHelper.call(config, url, ws).flatMap { _ match {
      case Success(json) =>
        Logger.debug(s"getOption:Success -> $json")
        Json.fromJson[T](json).asOpt.map(j => Future.successful(Some(j))).getOrElse(Future.failed(new RuntimeException(s"Could not parse $json")))
      case Failure(e) =>
        Logger.debug(s"getOption:Failure -> $e")
        Future.failed(e)
    }}
  }
}

object JiraWSHelper {
  import scala.async.Async.{async, await}
      
    def call(config:JiraConfiguration, url:String, ws:WSClient)(implicit auth: JiraAuthentication, executionContext: ExecutionContext): Future[Try[JsValue]] = {
      auth match {
        case oauth: OAuthAuthentication =>
          callWithOAuth(config, url, oauth)
        case basicAuth: BasicAuthentication => 
          callWithBasicAuth(config, url, ws, basicAuth)
      }
    }
    
    def callWithOAuth(config:JiraConfiguration, url:String, auth:OAuthAuthentication)(implicit executionContext: ExecutionContext):Future[Try[JsValue]] = {
      async{
        try {
          val accessor = OAuthUtil.getAccessor(config.baseUrl, auth.consumerKey, auth.privateKey, "")
           val client = new OAuthClient(new HttpClient4());
           val response = client.invoke(accessor, url, java.util.Collections.emptySet())       
           Success(Json.parse(response.readBodyAsString()))
        }
        catch {
          case e:Exception => Failure(e)
        }
      }
    }
    
    def callWithBasicAuth(config:JiraConfiguration, url:String, ws:WSClient, auth:BasicAuthentication)(implicit executionContext: ExecutionContext): Future[Try[JsValue]] = {
      val pair = s"${auth.username}:${auth.password}"
      val encPart = new String(Base64.encodeBase64(pair.getBytes("utf-8")), "utf-8")
      val enc = s"Basic $encPart"
      
      ws.url(url).withHeaders((headers :+ ("Authorization" -> enc)) : _*).get.map {resp =>
        resp.status match {
          case HttpStatus.SC_OK => Success(resp.json)
          case error => Failure(new IOException(s"Http status:$error"))
        }
      }
    }

    def headers() = {
      Seq(("Content-Type" -> "application/json"))
    }  
}

//object JiraApiServiceImpl extends JiraApiServiceImpl