name := "dribbble_user_tool"

version := "1.0"

lazy val `dribbble_user_tool` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq( ws )