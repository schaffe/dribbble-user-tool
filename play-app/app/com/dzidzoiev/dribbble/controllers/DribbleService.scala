package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import com.dzidzoiev.dribbble.controllers.paging.PagingProvider

import scala.concurrent.Future


class DribbleService @Inject()(
                                userPaging: PagingProvider[User],
                                shotPaging: PagingProvider[Shot],
                                restClient: DribbleRestClient
                              ) {

  def getFollowers(id: String) = {
    val getFollowersFun: (Int, Int) => Future[List[User]] = restClient.getFollowers(id)
    userPaging.fetchWithPaging(getFollowersFun)
  }

  def getShots(id:String) = {
    val getShotsFun: (Int, Int) => Future[List[Shot]] = restClient.getShots(id)
    shotPaging.fetchWithPaging(getShotsFun)
  }

  def getLikes(id: String) = {
    val getLikesFun: (Int, Int) => Future[List[User]] = restClient.getLikes(id)
    userPaging.fetchWithPaging(getLikesFun)
  }


}
