package com.dzidzoiev.dribbble.controllers.paging

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.contrib.pattern.Aggregator
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

class PagingProvider[T] @Inject()(system: ActorSystem,
                                  @Named("max-pages") limit: Int,
                                  @Named("pagesize") pagesize: Int
                                 ) {

  case class Result(result: List[T])

  type FetchFunction = (Int, Int) => Future[List[T]]

  def fetchWithPaging(func: FetchFunction): Future[List[T]] = {
    val p = Promise[List[T]]
    val paging: ActorRef = system.actorOf(Props(new PagedFetchActor))
    val replyTo = system.actorOf(Props(new Actor {
      def receive = {
        case reply: Result =>
          p.success(reply.result)
          context.stop(self)
        case e: Exception =>
          p.failure(e)
          context.stop(self)
      }
    }))
    paging.tell(func, sender = replyTo)
    p.future
  }

  class PagedFetchActor extends Actor with Aggregator {

    val values = ListBuffer.empty[T]
    var resultRef = ActorRef.noSender

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
      resultRef ! Result(values.toList)
      context.stop(self)
    }

    case class GetPage(fetch: FetchFunction, page: Int)

  }

}