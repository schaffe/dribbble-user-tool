package com.dzidzoiev.dribbble.controllers

import javax.inject.Inject

import com.dzidzoiev.dribbble.controllers.paging.PagingProvider

import scala.concurrent.Future


class DribbleService @Inject()(
                                paging: PagingProvider[User],
                                client: DribbleRestClient
                              ) {

  def getFollowers(id: String) = {
    val func: (Int, Int) => Future[List[User]] = client.getFollowers(id)
    paging.fetchWithPaging(func)
  }

}
