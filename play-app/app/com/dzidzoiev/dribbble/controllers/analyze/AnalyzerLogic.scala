package com.dzidzoiev.dribbble.controllers.analyze

import com.dzidzoiev.dribbble.controllers.User

object AnalyzerLogic {

  def groupByAndSort(users: List[User], limit:Int) = {
    val likers: Seq[Liker] = groupById(users)
    sortByFrequency(likers, limit)
  }

  def groupById(users: List[User]): Seq[Liker] = {
    users.groupBy(_.id)
      .toSeq
      .map(group => Liker(group._2.size, group._2.head))
  }

  def sortByFrequency(likers: Seq[Liker], limit: Int) = {
    likers.sortBy(liker => liker.likes)
      .reverse
      .slice(0, limit)
  }
}
