package com.dzidzoiev.dribbble.controllers

import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, Semaphore, TimeUnit}
import javax.inject.Inject

import com.dzidzoiev.dribbble.controllers.infrastucture.DribbleAuthKey
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcCurlRequestLogger

import scala.collection.mutable
import scala.concurrent.{Future, Promise, blocking}
import scala.util.{Failure, Success}

class DribbleRestClient @Inject()(ws: WSClient, @DribbleAuthKey key: String) {
  implicit val userReads = Json.reads[User]
  implicit val followerReads = Json.reads[Follower]
  implicit val shotReads = Json.reads[Shot]
  implicit val likeReads = Json.reads[Like]

  val baseUrl = "https://api.dribbble.com/v1"

  def getFollowers(id: String)(page: Int, pagesize: Int): Future[List[User]] = {
    doPagedRequest("/users/" + id + "/followers", page, pagesize)
      .map(userJson => userJson.validate[List[Follower]].get.map(f => f.follower))
  }

  def getShots(id: String)(page: Int, pagesize: Int): Future[List[Shot]] = {
    doPagedRequest("/users/" + id + "/shots", page, pagesize)
      .map(userJson => userJson.validate[List[Shot]].get)
  }

  def getLikes(id: String)(page: Int, pagesize: Int): Future[List[User]] = {
    doPagedRequest("/shots/" + id + "/likes", page, pagesize)
      .map(userJson => userJson.validate[List[Like]].get.map(like => like.user))
  }


  private def doPagedRequest(urlPath: String, page: Int, pagesize: Int): Future[JsValue] = {
    val promise = Promise[JsValue]()
    Future {
      ApiLock.acquire()
      ws.url(baseUrl + urlPath)
        .withRequestFilter(AhcCurlRequestLogger())
        .withQueryString(("per_page", pagesize))
        .withQueryString(("page", page))
        .withHeaders(("Authorization", "Bearer " + key))
        .get()
        .map({
          case resp if resp.status == 200 =>
            resp.json
          case error => throw DribbleException(error.json)
        })
        .onComplete({
          case Success(result) => promise.success(result)
          case Failure(error) => promise.failure(error)
        })
    }
    promise.future
  }

  /**
    * Lock based on low-level concurrency mechanics,
    * TODO: replace with actors and make available using multiple API keys
    */
  object ApiLock {
    val API_LIMIT = 60
    private val semaphore: Semaphore = new Semaphore(API_LIMIT)
    private val semaphoreReset = new ScheduledThreadPoolExecutor(1)
    private val futureMap = mutable.Map.empty[ReleaseTask, ScheduledFuture[_]]

    case class ReleaseTask() extends Runnable {
      override def run(): Unit = blocking {
        semaphore.release()
        futureMap.remove(this).get.cancel(false)
      }
    }

    def acquire() = blocking {
      val task = ReleaseTask()
      val future = semaphoreReset.scheduleAtFixedRate(task, 0, 60, TimeUnit.SECONDS)
      futureMap += (task -> future)
      semaphore.acquire()
    }
  }

  implicit def intToString(int: Int): String = int.toString
}