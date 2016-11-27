package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class DribbbleController @Inject()(dribbleService: DribbleService) extends Controller {
  implicit val userWrites = Json.writes[User]
  implicit val followerWrites = Json.writes[Follower]

  def getFollowers(userId: String) = Action.async {
    dribbleService.getFollowers(userId)
      .map(followers => Ok(Json.toJson(followers)))
//      .recover(case e: DribbleException => InternalServerError(e))
  }
}

class DribbleService @Inject()(client: DribbleRestClient) {

  def getFollowers(id: String) = client.getFollowers(id)

}

class DribbleRestClient @Inject()(ws: WSClient) {
  implicit val userReads = Json.reads[User]
  implicit val followerReads = Json.reads[Follower]

  val baseUrl = "https://api.dribbble.com/v1"
  val authKey = "b461e4de7c623018749a55b7e6eb5e8fe7e0ba0b6950764cb4c2552bff013e3c"

  def getFollowers(id: String): Future[List[Follower]] = {
    ws.url(baseUrl + "/users/" + id + "/followers")
      .withHeaders(("Authorization", "Bearer " + authKey))
      .get()
      .map({
        case resp if resp.status == 200 => resp.json
        case error => throw DribbleException(error.json)
      })
      .map(userJson => userJson.validate[List[Follower]].get)
  }
}

case class DribbleException(message: JsValue) extends RuntimeException(message.toString())

case class User(id: Long, name: String, username: String)

case class Follower(id: Long, follower: User)

