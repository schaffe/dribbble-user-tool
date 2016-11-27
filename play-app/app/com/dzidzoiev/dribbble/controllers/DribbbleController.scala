package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem, Props}
import akka.contrib.pattern.Aggregator
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.mvc.{Action, Controller}
import play.libs.akka.AkkaGuiceSupport

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{Future, Promise}

class DribbbleController @Inject()(dribbleService: DribbleService) extends Controller {
  implicit val userWrites = Json.writes[User]
  implicit val followerWrites = Json.writes[Follower]

  def getFollowers(userId: String) = Action.async {
    dribbleService.getFollowers(userId)
      .map(followers => Ok(Json.toJson(followers)))
    //      .recover(case e: DribbleException => InternalServerError(e))
  }
}

class UserActor @Inject()(client: DribbleRestClient) extends Actor with Aggregator {


  val pagesize = 10
  val values = ListBuffer.empty[User]

  expectOnce {
    case id: String => fetchPage(id, 0)

  }

  expect {
    case GetPage(id, page) => fetchPage(id, page)
  }

  def fetchPage(id: String, page: Int): Unit = {
    client.getFollowers(id, page, pagesize).onSuccess {
      case users if users.isEmpty =>
        context.parent ! values.toList
        context.stop(self)
      case users =>
        values ++= users
        self ! GetPage(id, page + 1)
    }
  }

  case class GetPage(id: String, page: Int)

}

class DribbleService @Inject()(system: ActorSystem) {

  def getFollowers(id: String) = {

    val userActor = system.actorOf(Props(classOf[UserActor]))

    val p = Promise[List[User]]
    val replyTo = system.actorOf(Props(new Actor {
      def receive = {
        case reply: List[User] =>
          p.success(reply)
          context.stop(self)
      }
    }))
    userActor.tell(id, sender = replyTo)
    p.future
  }

}

class DribbleRestClient @Inject()(ws: WSClient) {
  implicit val userReads = Json.reads[User]
  implicit val followerReads = Json.reads[Follower]

  val baseUrl = "https://api.dribbble.com/v1"
  val authKey = "b461e4de7c623018749a55b7e6eb5e8fe7e0ba0b6950764cb4c2552bff013e3c"

  def getFollowers(id: String, page: Int, pagesize: Int): Future[List[User]] = {
    ws.url(baseUrl + "/users/" + id + "/followers")
      .withRequestFilter(AhcCurlRequestLogger())
      .withQueryString(("per_page", pagesize))
      .withQueryString(("page", page))
      .withHeaders(("Authorization", "Bearer " + authKey))
      .get()
      .map({
        case resp if resp.status == 200 => resp.json
        case error => throw DribbleException(error.json)
      })
      .map(userJson => userJson.validate[List[Follower]].get.map(f => f.follower))
  }

  implicit def intToString(int: Int): String = int.toString
}

case class DribbleException(message: JsValue) extends RuntimeException(message.toString())

case class User(id: Long, name: String, username: String)

case class Follower(id: Long, follower: User)

