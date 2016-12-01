package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import com.dzidzoiev.dribbble.controllers.paging.PagingProvider

import scala.concurrent.Future


class DribbleService @Inject()(
                                userPaging: PagingProvider[User],
                                shotPaging: PagingProvider[Shot],
                                restClient: DribbleRestClient
                              ) {

  def getFollowers(userId: String) = {
    val getFollowersFun: (Int, Int) => Future[List[User]] = restClient.getFollowers(userId)
    userPaging.fetchWithPaging(getFollowersFun)
  }

  def getShots(userId:String) = {
    val getShotsFun: (Int, Int) => Future[List[Shot]] = restClient.getShots(userId)
    shotPaging.fetchWithPaging(getShotsFun)
  }

  def getLikes(shotId: String) = {
    val getLikesFun: (Int, Int) => Future[List[User]] = restClient.getLikes(shotId)
    userPaging.fetchWithPaging(getLikesFun)
  }


}
