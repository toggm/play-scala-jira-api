package services

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.routing.sird._
import models.JiraProject
import java.net.URI
import play.core.server.Server
import play.api.test.WsTestClient
import org.specs2.mutable.Specification
import play.api.Application
import play.api.libs.ws.WSClient

case class JiraApiServiceMock(ws: WSClient, config: JiraConfiguration) extends JiraApiServiceImpl

class JIRAAPIServiceSpec extends Specification {
  "getAllProjects" should {
    implicit val auth = OAuthAuthentication("123")
    implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
    val config = JiraConfiguration("")

    "get correct result with expand" in {
      val expand = "sadfasdf"

      Server.withRouter() {
        case GET(p"/rest/api/2/project?sadfasdf") => Action {
          Results.Ok(Json.arr(Json.obj("self" -> "http://test.com", "id" -> "1", "key" -> "proj1", "name" -> "projname")))
        }
      } { implicit port =>
        WsTestClient.withClient { implicit client =>
          val service = JiraApiServiceMock(client, config)
          val result = Await.result(
            service.getAllProjects(expand), 10.seconds)

          result.size === Seq(JiraProject(new URI("http://test.com"), "1", "proj1", "projname", None))
        }
      }
    }
  }
}