package controllers

import net.oauth._
import play.api.mvc.Controller
import java.net.URL
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.RequestToken
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import net.oauth.signature.RSA_SHA1
import com.google.common.collect.ImmutableList
import java.util.Collections
import net.oauth.client.OAuthClient
import net.oauth.client.httpclient4.HttpClient4
import net.oauth.OAuth.OAUTH_VERIFIER;
import scala.collection.JavaConversions._

case class OAuthRequestToken(token:String, tokenSecret:String, verifier: String, accessTokenUrl:String)
case class OAuthAccessToken(accessToken:String)

object OAuthController extends Controller {
    
    implicit val requestTokenFormat: Format[OAuthRequestToken] = Json.format[OAuthRequestToken]
    implicit val accessTokenFormat: Format[OAuthAccessToken] = Json.format[OAuthAccessToken]
    
    def index = Action {
      Ok(views.html.oauth(requestTokenForm, accessTokenForm))
    }
    
    case class RequestTokenData(baseUrl:String, consumerKey:String, privateKey:String, callbackUrl: String)
    case class AccessTokenData(baseUrl:String, consumerKey:String, privateKey:String, requestToken: String, tokenSecret:String, verifier:String)
    
    val requestTokenForm = Form(
      mapping(
        "baseUrl" -> text,
        "consumerKey" -> text,
        "privateKey" -> text,
        "callbackUrl" -> text
      )(RequestTokenData.apply)(RequestTokenData.unapply)
    )
    val accessTokenForm = Form(
      mapping(
        "baseUrl" -> text,
        "consumerKey" -> text,
        "privateKey" -> text,
        "requestToken" -> text,
        "tokenSecret" -> text,
        "verifier" -> text
      )(AccessTokenData.apply)(AccessTokenData.unapply)
    )
    
    def callbackParam(callback:String):List[OAuth.Parameter] = {
        if (callback == null || "".equals(callback)) {
            List()
        }
        else {
            List(new OAuth.Parameter(OAuth.OAUTH_CALLBACK, callback))
        }
    }
    
    /**
     * 
     */
    def obtainRequestToken = Action {      
      implicit requst => 
        requestTokenForm.bindFromRequest.fold(formWithErrors => {
          BadRequest(formWithErrors.toString)
        },
        data => {        
            Logger.warn(s"obtainRequestToken: $data")
            
            try {
              val oAuthClient = new OAuthClient(new HttpClient4())
              val callBack = callbackParam(data.callbackUrl)
  
              val accessor = getAccessor(data.baseUrl, data.consumerKey, data.privateKey, data.callbackUrl)
              val message = oAuthClient.getRequestTokenResponse(accessor, "POST", callBack.toList)
              
              val token = accessor.requestToken;
              val secret = accessor.tokenSecret;
              val verifier = message.getParameter(OAUTH_VERIFIER);
              
              val url = s"${data.baseUrl}/plugins/servlet/oauth/authorize?oauth_token=${token}"
              Ok(Json.toJson(OAuthRequestToken(token, secret, verifier, url)))
            }
            catch {
              case e:Exception => 
                Logger.warn("Didn't obtain request token", e)
                BadRequest(e.getMessage)
            }
        })
    }
    
    def obtainAccessToken = Action {
      implicit request =>
        accessTokenForm.bindFromRequest.fold(formWithErrors => {
          BadRequest(formWithErrors.toString)
        },
        data => {                
          try {
            val accessor = getAccessor(data.baseUrl, data.consumerKey, data.privateKey, "");
            val client = new OAuthClient(new HttpClient4());
            accessor.requestToken = data.requestToken;
            accessor.tokenSecret = data.tokenSecret;
            val message = client.getAccessToken(accessor, "POST",
                    ImmutableList.of(new OAuth.Parameter(OAuth.OAUTH_VERIFIER, data.verifier)));
            Ok(Json.toJson(OAuthAccessToken(message.getToken)))
          }
          catch {
            case e:Exception =>
              Logger.warn("Didn't obtain access token", e)
              BadRequest(e.getMessage)
          }         
        })
    }
    
     def getAccessor(baseUrl:String, consumerKey:String, privateKey:String, callback:String):OAuthAccessor = {
        val serviceProvider = new OAuthServiceProvider(baseUrl + "/plugins/servlet/oauth/request-token", baseUrl + "/plugins/servlet/oauth/authorize", baseUrl + "/plugins/servlet/oauth/access-token");
        val consumer = new OAuthConsumer(callback, consumerKey, null, serviceProvider);
        consumer.setProperty(RSA_SHA1.PRIVATE_KEY, privateKey);
        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
        new OAuthAccessor(consumer);
    }
}

