package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import com.dzidzoiev.dribbble.controllers.di.DribbleAuthKey
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcCurlRequestLogger

import scala.concurrent.Future

class DribbleRestClient @Inject()(ws: WSClient, @DribbleAuthKey key: String) {
  implicit val userReads = Json.reads[User]
  implicit val followerReads = Json.reads[Follower]
  implicit val shotReads = Json.reads[Shot]

  val baseUrl = "https://api.dribbble.com/v1"

  def getFollowers(id: String)( page: Int, pagesize: Int): Future[List[User]] = {
    doPagedRequest("/users/" + id + "/followers", page, pagesize)
      .map(userJson => userJson.validate[List[Follower]].get.map(f => f.follower))
  }

  def getShots(id: String, page: Int, pagesize: Int): Future[List[Shot]] = {
    doPagedRequest("/users/" + id + "/shots", page, pagesize)
      .map(userJson => userJson.validate[List[Shot]].get)
  }

  private def doPagedRequest(urlPath: String, page: Int, pagesize: Int): Future[JsValue] = {
    ws.url(baseUrl + urlPath)
      .withRequestFilter(AhcCurlRequestLogger())
      .withQueryString(("per_page", pagesize))
      .withQueryString(("page", page))
      .withHeaders(("Authorization", "Bearer " + key))
      .get()
      .map({
        case resp if resp.status == 200 => resp.json
        case error => throw DribbleException(error.json)
      })
  }

  implicit def intToString(int: Int): String = int.toString
}