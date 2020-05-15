inThisBuild(
  List(
    organization := "com.geirsson",
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
    ),
    buildInfoPackage := "gitflip.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version
    )
  )
  .enablePlugins(BuildInfoPlugin)
