package com.dzidzoiev.dribbble.controllers.analyze

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.dzidzoiev.dribbble.controllers.{DribbleService, Shot, User}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

class AnalyzerService @Inject()(
                                 actorSystem: ActorSystem,
                                 dribbleService: DribbleService
                               ) {
  val LIMIT_RESULT_SIZE = 10

  def analyzeLikes(userId: String) = {

    val p = Promise[Seq[Liker]]
    val replyTo = actorSystem.actorOf(Props(new Actor {
      def receive = {
        case reply: Result =>
          p.success(reply.likers)
          context.stop(self)
        case exception: Exception =>
          p.failure(exception)
          context.stop(self)
      }
    }))

    val aggregator: ActorRef = actorSystem.actorOf(Props(new LikesAggregator(replyTo)))

    val followersFuture = dribbleService.getFollowers(userId)
    followersFuture.onSuccess({
      case followers =>
        extractShots(followers)
        aggregator ! NotifyFollowersCount(followers.size)
    })
    followersFuture.onFailure({
      case exception =>
        aggregator ! exception
    })

    def extractShots(followers: List[User]) = followers
      .map(follower => dribbleService.getShots(follower.id))
      .foreach(future => {
        future.onSuccess({
          case shots =>
            extractLikes(shots)
            aggregator ! NotifyShotsCount(shots.size)
            aggregator ! NotifyFollowerProcessed
        })
        future.onFailure({
          case exception => aggregator ! exception
        })
      })

    def extractLikes(shots: List[Shot]) = shots
      .map(shot => dribbleService.getLikes(shot.id))
      .foreach(future => {
        future.onSuccess({
          case likes =>
            aggregator ! NotifyShotProcessed(likes)
        })
        future.onFailure({
          case exception => aggregator ! exception
        })
      })

    p.future
  }

  class LikesAggregator(val resultListener: ActorRef) extends Actor {
    var followersCount = 0
    var shotsCount = 0
    val likers = ListBuffer.empty[User]

    override def receive: Receive = {
      case NotifyFollowersCount(n) =>
        followersCount = n
      case NotifyFollowerProcessed =>
        followersCount -= 1
        checkReportResult()
      case NotifyShotsCount(n) =>
        shotsCount += n
      case NotifyShotProcessed(likes) =>
        shotsCount -=1
        likers ++= likes
        checkReportResult()
      case exception: Exception =>
        resultListener ! exception
        context.stop(self)
    }

    def checkReportResult(): Unit = {
      if (shotsCount == 0 && followersCount == 0) {
        resultListener ! Result(analyze(likers))
        context.stop(self)
      }
    }

    def analyze(likers: ListBuffer[User]): Seq[Liker] = {
      AnalyzerLogic.groupByAndSort(likers.toList, LIMIT_RESULT_SIZE)
    }
  }

  case class NotifyFollowersCount(n: Int)

  case object NotifyFollowerProcessed

  case class NotifyShotsCount(n: Int)

  case class NotifyShotProcessed(likes: List[User])

  case class Result(likers: Seq[Liker])

  implicit def longToString(long: Long): String = long.toString
}
