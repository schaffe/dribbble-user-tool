package com.dzidzoiev.dribbble.controllers

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.contrib.pattern.Aggregator
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise

class DribbleService @Inject()(system: ActorSystem, @Named("user-actor") userActor: ActorRef) {

  def getFollowers(id: String) = {

    val p = Promise[List[User]]
    val replyTo = system.actorOf(Props(new Actor {
      def receive = {
        case reply: List[User] =>
          p.success(reply)
          context.stop(self)
        case e : Exception =>
          p.failure(e)
          context.stop(self)
      }
    }))
    userActor.tell(id, sender = replyTo)
    p.future
  }

}

class UserActor @Inject()(client: DribbleRestClient) extends Actor with Aggregator {

  val pagesize = 10
  val maxpage = 2
  val values = ListBuffer.empty[User]
  var resultRef = ActorRef.noSender

  expectOnce {
    case id: String => fetchPage(id, 0)
      this.resultRef = context.sender()

  }

  expect {
    case GetPage(id, page) => fetchPage(id, page)
  }

  def fetchPage(id: String, page: Int): Unit = {
    if (page >= maxpage) reportResult
    else {
      val future = client.getFollowers(id, page, pagesize)
      future.onSuccess {
          case users if users.isEmpty =>
            reportResult
          case users =>
            values ++= users
            self ! GetPage(id, page + 1)
        }
      future.onFailure {
        case e : Exception =>
          resultRef ! e
          context.stop(self)
      }
    }

  }

  def reportResult: Unit = {
    resultRef ! values.toList
    context.stop(self)
  }

  case class GetPage(id: String, page: Int)

}
