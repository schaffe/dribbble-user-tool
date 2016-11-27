package com.dzidzoiev.dribbble.controllers.di

import javax.inject.{Inject, Named, Singleton}

import com.dzidzoiev.dribbble.controllers.UserActor
import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import play.libs.akka.AkkaGuiceSupport

class GuiceConfigModule extends AbstractModule with AkkaGuiceSupport {

  def configure() = {
    bindActor(classOf[UserActor], "user-actor")
  }

  @Provides
  @DribbleAuthKey
  @Singleton
  def provideAuthKey(config: Configuration): String = {
    config.getString("dribble.auth-key").get
  }

  @Provides
  @Named("max-pages")
  @Singleton
  def provideMaxPages(config: Configuration): Int = {
    config.getInt("dribble.max-pages").getOrElse(1)
  }

  @Provides
  @Named("pagesize")
  @Singleton
  def provideDefaultPageSize(config: Configuration): Int = {
    config.getInt("dribble.pagesize").getOrElse(10)
  }
}



