package controllers

import play.api.mvc.Controller
import java.net.URL
import play.api.mvc.Action
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.ServiceInfo
import play.api.libs.json._
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.RequestToken
import play.api.Logger

case class OAuthRequestToken(token:String, tokenSecret:String, accessTokenUrl:String)
case class OAuthAccessToken(accessToken:String)

trait OAuthController {
  self: Controller =>
    
    implicit val requestTokenFormat: Format[OAuthRequestToken] = Json.format[OAuthRequestToken]
    implicit val accessTokenFormat: Format[OAuthAccessToken] = Json.format[OAuthAccessToken]
    
    def index = Action {
      Ok(views.html.oauth())
    }
    
    def obtainRequestToken(baseUrl:String, consumerKey:String, publicKey:String, callbackUrl: String) = Action {
      requst => 

      oauth(baseUrl, consumerKey, publicKey).retrieveRequestToken(callbackUrl) match {
        case Right(t) => {
          val url = s"${baseUrl}/jira/plugins/servlet/oauth/authorize?oauth_token=${t.token}"
          Ok(Json.toJson(OAuthRequestToken(t.token, t.secret, url)))
        }
        case Left(e) =>
          Logger.warn("Didn't obtain request token", e)
          BadRequest(e.getMessage)
      }
    }
    
    def obtainAccessToken(baseUrl:String, consumerKey:String, publicKey:String, requestToken: String, tokenSecret:String, verifier:String) = Action {
      val tokenPair = RequestToken(requestToken, tokenSecret)
      oauth(baseUrl, consumerKey, publicKey).retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          Ok(Json.toJson(OAuthAccessToken(t.token)))
        }
        case Left(e) =>
          Logger.warn("Didn't obtain access token", e)
          BadRequest(e.getMessage)
      }
    }
  
    def oauth(baseUrl:String, consumerKey:String, publicKey:String): OAuth = {
      val key = ConsumerKey(consumerKey, publicKey)
      OAuth(ServiceInfo(
          baseUrl + "/plugins/servlet/oauth/request-token",
          baseUrl + "/plugins/servlet/oauth/access-token",
          baseUrl + "/plugins/servlet/oauth/authorize", key), 
          true)
    }
}

object OAuthController extends OAuthController with Controller