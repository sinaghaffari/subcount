package controllers

import java.util.UUID

import com.google.inject.Inject
import play.api._
import play.api.cache.CacheApi
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import io.kanaka.monadic.dsl._
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

import utils.FutureFailIf._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject() (ws: WSClient, cache: CacheApi, config: Configuration) extends Controller {
  val clientID = config.underlying.getString("subcount.clientID")
  val redirectURI = config.underlying.getString("subcount.redirectURI")
  val scope = config.underlying.getString("subcount.scope")
  val clientSecret = config.underlying.getString("subcount.clientSecret")

  def signup = Action {
    val state = UUID.randomUUID().toString
    cache.set(state, false)
    val redirectURL = s"https://api.twitch.tv/kraken/oauth2/authorize?response_type=code&client_id=$clientID&redirect_uri=$redirectURI&scope=$scope&state=$state"
    Redirect(redirectURL)
  }

  def signedup(code: String, scope: String, state: String) = Action.async {
    println("test test!")
    def sendPostToken = ws.url("https://api.twitch.tv/kraken/oauth2/token").post(
      Map(
        "client_id" -> Seq(clientID),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("authorization_code"),
        "redirect_uri" -> Seq(redirectURI),
        "code" -> Seq(code),
        "state" -> Seq(state)
      )
    )
    def sendGetUsername(token: String) = ws.url("https://api.twitch.tv/kraken").withHeaders(("Authorization", s"OAuth $token")).get()
    for {
      cacheCheck <- cache.get[Boolean](state)                             ?| Redirect(routes.Application.signup())
      tokenResponse <- sendPostToken.failIf(_.status != 200)(e => new Exception("error.")) ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      tokenObject <- tokenResponse.json.validate[JsObject]                ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      token <- (tokenResponse.json \ "access_token").validate[String]     ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      usernameResponse <- sendGetUsername(token).failIf(_.status != 200)(e => new Exception("error.")) ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      username <- (usernameResponse.json \ "token" \ "user_name").validate[String]  ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      saveResponse <- ws.url(s"http://localhost:9200/subcount/auth/$username").put(tokenObject ++ Json.obj("created_at" -> DateTime.now().toString)).failIf(s => s.status != 200 && s.status != 201)(ex => new Exception("error")) ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
    } yield {
      Ok(s"Success!! Get your subcount at: http://fancyfetus.io/subcount/$username")
    }
  }

  def subcount(channel: String) = Action.async {
    for {
      tokenResponse <- ws.url(s"http://localhost:9200/subcount/auth/$channel").get().failIf(_.status != 200)(e => new Exception("Invalid")) ?| BadRequest(s"$channel has not been found. Please register at ${routes.Application.signup().url}")
      token <- (tokenResponse.json \ "_source" \ "access_token").validate[String] ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      subResponse <- ws.url(s"https://api.twitch.tv/kraken/channels/$channel/subscriptions").withHeaders(("Authorization", s"OAuth $token")).get().failIf(_.status != 200)(e => new Exception("Invalid")) ?| InternalServerError("Internal Server Error. Please contact fancyfetus@gmail.com for assistance.")
      subscribers <- (subResponse.json \ "_total").validate[Int] ?| BadRequest(s"$channel does not have a subscription program.")
    } yield Ok(subscribers.toString)
  }
}