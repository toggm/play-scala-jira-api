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
import net.oauth.OAuth.OAUTH_VERIFIER
import scala.collection.JavaConversions._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import utils.OAuthUtil

case class OAuthRequestToken(token:String, tokenSecret:String, verifier: String, accessTokenUrl:String)
case class OAuthAccessToken(accessToken:String)
case class RequestTokenData(baseUrl:String, consumerKey:String, privateKey:String, callbackUrl: String)
case class AccessTokenData(baseUrl:String, consumerKey:String, privateKey:String, requestToken: String, tokenSecret:String, verifier:String)
    

object OAuthController extends Controller {
    
    implicit val requestTokenFormat: Format[OAuthRequestToken] = Json.format[OAuthRequestToken]
    implicit val accessTokenFormat: Format[OAuthAccessToken] = Json.format[OAuthAccessToken]
    
    implicit val requestTokenDataFormat: Format[RequestTokenData] = Json.format[RequestTokenData]
    implicit val accessTokenDataFormat: Format[AccessTokenData] = Json.format[AccessTokenData]
    
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

    
    def index = Action {
      Ok(views.html.oauth(requestTokenForm))
    }   
           
    def callbackParam(callback:String):List[OAuth.Parameter] = {
        if (callback == null || "".equals(callback)) {
            List()
        }
        else {
            List(new OAuth.Parameter(OAuth.OAUTH_CALLBACK, callback))
        }
    }
    
    def obtainRequestTokenJson = Action(parse.json) { 
      implicit request =>
      request.body.validate[RequestTokenData].map{ 
        case (data) => 
          obtainRequestToken(data) match {
            case Success(token) =>
              Ok(Json.toJson(token))
            case Failure(e) =>
              BadRequest(e.getMessage)
          }
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }      
    }
    
    def obtainRequestTokenForm = Action {      
      implicit request =>
        requestTokenForm.bindFromRequest.fold(formWithErrors => {
          Ok(views.html.oauth(formWithErrors))
        },
        data => {
          obtainRequestToken(data) match {
            case Success(token) =>
              val accessTokenData  =AccessTokenData(data.baseUrl, data.consumerKey, data.privateKey, token.token, token.tokenSecret, null)
              val prefilled = accessTokenForm.fill(accessTokenData)
              Ok(views.html.accessToken(token.accessTokenUrl, prefilled))
            case Failure(e) =>
              val formWithErrors = requestTokenForm.withError(FormError.apply("", e.getMessage))
              Ok(views.html.oauth(formWithErrors))
          }          
        })
    }
    
    /**
     * 
     */
    def obtainRequestToken(data:RequestTokenData):Try[OAuthRequestToken] = { 
            Logger.debug(s"obtainRequestToken: $data")           
        try {
          val oAuthClient = new OAuthClient(new HttpClient4())
          val callBack = callbackParam(data.callbackUrl)

          val accessor = OAuthUtil.getAccessor(data.baseUrl, data.consumerKey, data.privateKey, data.callbackUrl)
          val message = oAuthClient.getRequestTokenResponse(accessor, "POST", callBack.toList)
          
          val token = accessor.requestToken;
          val secret = accessor.tokenSecret;
          val verifier = message.getParameter(OAUTH_VERIFIER);
          
          val url = s"${data.baseUrl}/plugins/servlet/oauth/authorize?oauth_token=${token}"
          Success(OAuthRequestToken(token, secret, verifier, url))
        }
        catch {
          case e:Exception => 
            Logger.warn("Didn't obtain request token", e)
            Failure(e)
        }
      
    }
    
    def obtainAccessTokenJson = Action(parse.json) { 
      implicit request =>
        request.body.validate[AccessTokenData].map{ 
          case (data) => 
            obtainAccessToken(data) match {
              case Success(token) =>
                Ok(Json.toJson(token))
              case Failure(e) =>
                BadRequest(e.getMessage)
            }
      }.recoverTotal{
        e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
      }                
    }
    
    def obtainAccessTokenForm = Action {
      implicit request =>
        accessTokenForm.bindFromRequest.fold(formWithErrors => {
          Ok(views.html.accessToken("", formWithErrors))
        },
        data => {
          obtainAccessToken(data) match {
            case Success(token) =>
              Ok(views.html.result(token.accessToken))
            case Failure(e) =>
              val formWithErrors = accessTokenForm.withError(FormError.apply("", e.getMessage))
              Ok(views.html.accessToken("", formWithErrors))
          }
        })
    }
    
    def obtainAccessToken(data:AccessTokenData):Try[OAuthAccessToken] =  {
          try {
            val accessor = OAuthUtil.getAccessor(data.baseUrl, data.consumerKey, data.privateKey, "");
            val client = new OAuthClient(new HttpClient4());
            accessor.requestToken = data.requestToken;
            accessor.tokenSecret = data.tokenSecret;
            val message = client.getAccessToken(accessor, "POST",
                    ImmutableList.of(new OAuth.Parameter(OAuth.OAUTH_VERIFIER, data.verifier)));
            Success(OAuthAccessToken(message.getToken))
          }
          catch {
            case e:Exception =>
              Logger.warn("Didn't obtain access token", e)
              Failure(e)
          }        
    }    
}

