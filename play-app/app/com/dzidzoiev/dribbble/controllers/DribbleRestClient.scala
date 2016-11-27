package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

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