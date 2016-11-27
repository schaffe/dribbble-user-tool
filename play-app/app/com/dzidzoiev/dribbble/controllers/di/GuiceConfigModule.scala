package com.dzidzoiev.dribbble.controllers.di

import com.dzidzoiev.dribbble.controllers.UserActor
import com.google.inject.AbstractModule
import play.libs.akka.AkkaGuiceSupport

class GuiceConfigModule extends AbstractModule with AkkaGuiceSupport {
  def configure() = {
    bindActor(classOf[UserActor], "user-actor")
  }
}
