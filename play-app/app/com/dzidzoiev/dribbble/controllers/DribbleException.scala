package com.dzidzoiev.dribbble.controllers

import play.api.libs.json.JsValue

case class DribbleException(message: JsValue) extends RuntimeException(message.toString())

