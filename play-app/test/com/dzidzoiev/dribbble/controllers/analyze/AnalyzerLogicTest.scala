package com.dzidzoiev.dribbble.controllers.analyze

import com.dzidzoiev.dribbble.controllers.User
import org.scalatest.FunSuite

class AnalyzerLogicTest extends FunSuite {

  val user1: User = User(1, "A", "AA")
  val user2: User = User(2, "B", "BB")
  val user3: User = User(3, "C", "CC")

  test("empty groupBy should produce empty Seq") {
    assert(AnalyzerLogic.groupById(List.empty).equals(Seq.empty))
  }

  test("group by one element") {
    val user1: User = User(1, "", "")
    val users = List(user1)
    val expected = Seq(Liker(1, user1))
    assert(AnalyzerLogic.groupById(users).equals(expected))
  }

  test("group by distinct elements") {

    val users = List(user1, user2, user3)
    val expected = Seq(Liker(1, user1), Liker(1, user2), Liker(1, user3))

    val actual: Seq[Liker] = AnalyzerLogic.groupById(users)
    assert(actual.size == expected.size)
    expected.foreach(liker => {
      assert(actual.contains(liker))
    })
  }

  test("group by repeatable elements") {
    val users = List(user1, user1, user1, user2, user3, user2)
    val expected = Seq(Liker(3, user1), Liker(2, user2), Liker(1, user3))

    val actual: Seq[Liker] = AnalyzerLogic.groupById(users)
    assert(actual.size == expected.size)
    expected.foreach(liker => {
      assert(actual.contains(liker))
    })
  }

  test("sort by frequency") {
    val unordered = Seq(Liker(0, user1), Liker(1, user2), Liker(2, user3))
    val expected = Seq(Liker(2, user3), Liker(1, user2), Liker(0, user1))

    val actual: Seq[Liker] = AnalyzerLogic.sortByFrequency(unordered, 10)
    assert(expected.equals(actual))
  }

  test("limit to 1") {
    val unordered = Seq(Liker(0, user1), Liker(1, user2), Liker(2, user3))
    val expected = Seq(Liker(2, user3))

    val actual: Seq[Liker] = AnalyzerLogic.sortByFrequency(unordered, 1)
    assert(expected.equals(actual))
  }

  test("limit to 0") {
    val unordered = Seq(Liker(0, user1), Liker(1, user2), Liker(2, user3))

    val actual: Seq[Liker] = AnalyzerLogic.sortByFrequency(unordered, 0)
    assert(actual.isEmpty)
  }


}
