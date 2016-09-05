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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject() (ws: WSClient, cache: CacheApi, config: Configuration) extends Controller {
  val clientID = config.underlying.getString("subcount.clientID")
  val redirectURI = config.underlying.getString("subcount.redirectURI")
  val scope = config.underlying.getString("subcount.scope")
  val clientSecret = config.underlying.getString("subcount.clientSecret")

  def register = Action {
    val state = UUID.randomUUID().toString
    cache.set(state, false)
    val redirectURL = s"https://api.twitch.tv/kraken/oauth2/authorize?response_type=code&client_id=$clientID&redirect_uri=$redirectURI&scope=$scope&state=$state"
    Redirect(redirectURL)
  }

  def registered(code: String, scope: String, state: String) = Action.async {
    def sendPostToken = ws.url("https://api.twitch.tv/kraken/oauth2/token").post(
      Map(
        "client_id" -> Seq(clientID),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("authorization_code"),
        "redirect_uri" -> Seq(redirectURI),
        "scope" -> Seq(scope),
        "state" -> Seq(state)
      )
    )
    def sendGetUsername(token: String) = ws.url("https://api.twitch.tv/kraken").withHeaders(("Authorization", s"OAuth $token")).get()

    for {
      cacheCheck <- cache.get[Boolean](state)                             ?| BadRequest
      tokenResponse <- sendPostToken                                      ?| BadRequest if tokenResponse.status != 200
      tokenObject <- tokenResponse.json.validate[JsObject]                ?| BadRequest
      token <- (tokenResponse.json \ "access_token").validate[String]     ?| BadRequest
      usernameResponse <- sendGetUsername(token)                          ?| BadRequest if usernameResponse.status != 200
      username <- (usernameResponse.json \ "user_name").validate[String]  ?| BadRequest
      saveResponse <- ws.url(s"http://localhost:9200/subcount/auth/$username").post(tokenObject ++ Json.obj("created_at" -> DateTime.now().toString)) ?| BadRequest
    } yield {
      println(cacheCheck)
      println(tokenResponse)
      println(tokenObject)
      println(token)
      println(usernameResponse)
      println(username)
      println(saveResponse)
      cache.remove(state)
      Ok(s"Success!! Get your subcount at: http://fancyfetus.io/subcount/$username")
    }
  }

  def subcount(channel: String) = Action.async {
//    for {
//      token <- ws.url("")
//    } yield
      Future(Ok("100"))
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}