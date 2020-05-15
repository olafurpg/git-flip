lazy val V = new {
  val version = "0.1.0-SNAPSHOT"
}
inThisBuild(
  List(
    organization := "com.geirsson",
    version := V.version
  )
)

skip in publish := true
crossScalaVersions := Nil

lazy val gitflip = project
  .settings(
    moduleName := "git-flip",
    mainClass := Some("gitflip.Gitflip"),
    scalaVersion := "2.13.2",
    libraryDependencies ++= List(
      "com.geirsson" %% "metaconfig-core" % "0.9.10"
    )
  )
