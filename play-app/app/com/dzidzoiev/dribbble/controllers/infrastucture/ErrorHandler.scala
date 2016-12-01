package com.dzidzoiev.dribbble.controllers.infrastucture

import javax.inject._

import com.dzidzoiev.dribbble.controllers.DribbleException
import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent._

@Singleton
class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {


  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      exception match {
        case DribbleException(_) => InternalServerError(exception.getMessage)
        case _ => InternalServerError("A server error occurred:  " + exception.getMessage)

      }
    )
  }

}