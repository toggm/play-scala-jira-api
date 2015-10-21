package services

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.test._
import org.apache.commons.codec.binary.Base64

object JiraWSHelperSpec extends PlaySpecification with Results {
  "Authentication headers" should {
    "Append BasicAuthentication credentials" in {
      val jiraws = new JiraWSHelper.JiraWS(null)
      val username = "userxx";
      val pwd = "pwdyy";
      val result = jiraws.headers(BasicAuthentication(username, pwd))

      result.size === 2
      result(0) === ("Content-Type", "application/json")
      result(1)._1 === "Authorization"

      val auth = result(1)._2.substring(6)
      val dec = new String(Base64.decodeBase64(auth), "utf-8")
      dec === s"$username:$pwd"
    }

    "Append OAuth token credentials" in {
      val jiraws = new JiraWSHelper.JiraWS(null)
      val token = "sadas231";
      val result = jiraws.headers(OAuthAuthentication(token))

      result.size === 2
      result(0) === ("Content-Type", "application/json")
      result(1) === ("oauth_token", token)
    }
  }
}