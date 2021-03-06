package services

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.routing.sird._
import models._
import java.net.URI
import play.core.server.Server
import play.api.test.WsTestClient
import org.specs2.mutable.Specification
import play.api.Application
import play.api.libs.ws.WSClient

case class JiraApiServiceMock(ws: WSClient, config: JiraConfiguration) extends JiraApiServiceImpl

class JIRAAPIServiceSpec extends Specification {
  implicit val auth = BasicAuthentication("key", "123")
  val config = JiraConfiguration("")
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  "getProjectVersions" should {

    "get correct result" in {
      val projectId = "123"

      Server.withRouter() {
        case GET(p"/rest/api/2/project/123/version") => Action {
          Results.Ok(Json.arr(Json.obj("self" -> "http://test.com", "id" -> "1", "description" -> "version1", "name" -> "1.0", "archived" -> false, "released" -> true)))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.getProjectVersions(projectId), 10.seconds)
          result === Seq(JiraVersion(new URI("http://test.com"), "1", "version1", "1.0", false, true))
        }
      }
    }
  }
  "getAllProjects" should {

    "get correct result" in {
      val expand = "sadfasdf"

      Server.withRouter() {
        case GET(p"/rest/api/2/project") => Action {
          Results.Ok(Json.arr(Json.obj("self" -> "http://test.com", "id" -> "1", "key" -> "proj1", "name" -> "projname")))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.getAllProjects(), 10.seconds)

          result === Seq(JiraProject(new URI("http://test.com"), "1", "proj1", "projname", None))
        }
      }
    }
  }

  "getVersions" should {

    "get correct result" in {
      val projectId = "123"

      Server.withRouter() {
        case GET(p"/rest/api/2/project/123/versions") => Action {
          Results.Ok(Json.arr(Json.obj("self" -> "http://test.com", "id" -> "1", "description" -> "version1", "name" -> "1.0", "archived" -> false, "released" -> true)))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.getVersions(projectId), 10.seconds)
          result === Seq(JiraVersion(new URI("http://test.com"), "1", "version1", "1.0", false, true))
        }
      }
    }
  }

  "findIssues" should {

    "get correct result" in {
      val jql = "key='123'"

      Server.withRouter() {
        case GET(p"/rest/api/2/search") => Action {
          val json = """{"expand":"names,schema","startAt":0,"maxResults":1,"total":42,"issues":[{"expand":"editmeta,renderedFields,transitions,changelog,operations","id":"11749","self":"https://test.com/rest/api/2/issue/11749","key":"LAS-42"}]}"""          
          Results.Ok(Json.parse(json))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.findIssues(jql), 10.seconds)
          result.expand === Some("names,schema")
          result.total === 42
          result.issues.size === 1
        }
      }
    }
    
    "parse without expand" in {
      val jql = "key='123'"

      Server.withRouter() {
        case GET(p"/rest/api/2/search") => Action {
          val json = """{"startAt":0,"maxResults":100,"total":22,"issues":[]}"""          
          Results.Ok(Json.parse(json))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.findIssues(jql), 10.seconds)
          result.expand === None
          result.total === 22
          result.issues.size === 0
        }
      }
    }
  }

}