package utils

import net.oauth.OAuthServiceProvider
import net.oauth.OAuthConsumer
import net.oauth.OAuthAccessor
import net.oauth.signature.RSA_SHA1
import net.oauth.OAuth

object OAuthUtil {
  def getAccessor(baseUrl:String, consumerKey:String, privateKey:String, callback:String):OAuthAccessor = {
        val serviceProvider = new OAuthServiceProvider(baseUrl + "/plugins/servlet/oauth/request-token", baseUrl + "/plugins/servlet/oauth/authorize", baseUrl + "/plugins/servlet/oauth/access-token");
        val consumer = new OAuthConsumer(callback, consumerKey, null, serviceProvider);
        consumer.setProperty(RSA_SHA1.PRIVATE_KEY, privateKey);
        consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
        new OAuthAccessor(consumer);
    }
}