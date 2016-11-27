package com.dzidzoiev.dribbble.controllers.paging

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.contrib.pattern.Aggregator
import com.dzidzoiev.dribbble.controllers.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

class PagingProvider[T] @Inject()(system: ActorSystem, @Named("paging") paging: ActorRef) {

  def fetchWithPaging(func: (Int, Int) => Future[List[T]]): Future[List[T]] = {
    val p = Promise[List[T]]
    val replyTo = system.actorOf(Props(new Actor {
      def receive = {
        case reply: List[T] =>
          p.success(reply)
          context.stop(self)
        case e: Exception =>
          p.failure(e)
          context.stop(self)
      }
    }))
    paging.tell(func, sender = replyTo)
    p.future
  }

}


class PagedFetchActor @Inject()(
                                 @Named("max-pages") limit: Int,
                                 @Named("pagesize") pagesize: Int
                               )
  extends Actor with Aggregator {

  val values = ListBuffer.empty[User]
  var resultRef = ActorRef.noSender

  type FetchFunction = (Int, Int) => Future[List[User]]

  expectOnce {
    case func: FetchFunction => fetchPage(func, 0)
      this.resultRef = context.sender()

  }

  expect {
    case GetPage(id, page) => fetchPage(id, page)
  }

  def fetchPage(fetch: FetchFunction, page: Int): Unit = {
    if (page >= limit)
      reportResult()
    else {
      val future = fetch(page, pagesize)
      future.onSuccess {
        case list if list.isEmpty =>
          reportResult()
        case batch =>
          values ++= batch
          self ! GetPage(fetch, page + 1)
      }
      future.onFailure {
        case e: Exception =>
          resultRef ! e
          context.stop(self)
      }
    }

  }

  def reportResult(): Unit = {
    resultRef ! values.toList
    context.stop(self)
  }

  case class GetPage(fetch: FetchFunction, page: Int)

}