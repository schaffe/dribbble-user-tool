package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class DribbleController @Inject()(dribbleService: DribbleService) extends Controller {
  implicit val userWrites = Json.writes[User]
  implicit val followerWrites = Json.writes[Follower]

  def getFollowers(userId: String) = Action.async {
    dribbleService.getFollowers(userId)
      .map(followers => Ok(Json.toJson(followers)))
    //      .recover(case e: DribbleException => InternalServerError(e))
  }
}








