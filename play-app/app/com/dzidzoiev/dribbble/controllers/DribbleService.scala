package com.dzidzoiev.dribbble.controllers

import javax.inject.{Inject, Named}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.contrib.pattern.Aggregator

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise

class UserActor @Inject()(client: DribbleRestClient) extends Actor with Aggregator {


  val pagesize = 10
  val maxpage = 2
  val values = ListBuffer.empty[User]
  var senderRef = ActorRef.noSender

  expectOnce {
    case id: String => fetchPage(id, 0)
      this.senderRef = context.sender()

  }

  expect {
    case GetPage(id, page) => fetchPage(id, page)
  }

  def fetchPage(id: String, page: Int): Unit = {
    if (page >= maxpage) reportResult
    else
      client.getFollowers(id, page, pagesize).onSuccess {
        case users if users.isEmpty =>
          reportResult
        case users =>
          values ++= users
          self ! GetPage(id, page + 1)
      }
  }

  def reportResult: Unit = {
    senderRef ! values.toList
    context.stop(self)
  }

  case class GetPage(id: String, page: Int)

}

class DribbleService @Inject()(system: ActorSystem, @Named("user-actor") userActor: ActorRef) {

  def getFollowers(id: String) = {

    val p = Promise[List[User]]
    val replyTo = system.actorOf(Props(new Actor {
      def receive = {
        case reply: List[User] =>
          p.success(reply)
          context.stop(self)
      }
    }))
    userActor.tell(id, sender = replyTo)
    p.future
  }

}
