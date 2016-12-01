package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

class DribbleController @Inject()(dribbleService: DribbleService) extends Controller {
  implicit val userWrites = Json.writes[User]
  implicit val shotWrites = Json.writes[Shot]

  def getFollowers(userId: String) = Action.async {
    dribbleService.getFollowers(userId)
      .map(followers => Ok(Json.toJson(followers)))
  }

  def getShots(shotId: String) = Action.async {
    dribbleService.getShots(shotId)
      .map(shots => Ok(Json.toJson(shots)))
  }

  def getLikes(likeId: String) = Action.async {
    dribbleService.getLikes(likeId)
      .map(likes => Ok(Json.toJson(likes)))
  }
}








