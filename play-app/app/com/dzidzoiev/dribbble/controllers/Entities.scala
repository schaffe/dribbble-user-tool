package com.dzidzoiev.dribbble.controllers

case class User(id: Long, name: String, username: String)

case class Follower(id: Long, follower: User)
