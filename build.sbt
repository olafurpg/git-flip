inThisBuild(
  List(
    organization := "com.geirsson",
  )
)

skip in publish := true
crossScalaVersions := Nil
addCommandAlias(
  "native-image",
  "; gitflip/graalvm-native-image:packageBin ; taskready"
)
commands += Command.command("taskready") { s =>
  import scala.sys.process._
  "say 'native-image ready'".!
  s
}

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
    ),
    mainClass in GraalVMNativeImage := Some(
      "gitflip.Gitflip",
    ),
    graalVMNativeImageOptions ++= {
      val reflectionFile =
        Keys.sourceDirectory.in(Compile).value./("graal")./("reflection.json")
      assert(reflectionFile.exists, "no such file: " + reflectionFile)
      List(
        "-H:+ReportUnsupportedElementsAtRuntime",
        "--initialize-at-build-time",
        "--initialize-at-run-time=metaconfig",
        "--no-server",
        "--enable-http",
        "--enable-https",
        "-H:EnableURLProtocols=http,https",
        "--enable-all-security-services",
        "--no-fallback",
        s"-H:ReflectionConfigurationFiles=$reflectionFile",
        "--allow-incomplete-classpath",
        "-H:+ReportExceptionStackTraces"
      )
    }
  )
  .enablePlugins(BuildInfoPlugin, GraalVMNativeImagePlugin)

