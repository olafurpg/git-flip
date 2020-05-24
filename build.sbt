inThisBuild(
  List(
    organization := "com.geirsson",
    scalacOptions += "-Yrangepos",
    useSuperShell := false
  )
)

skip in publish := true
crossScalaVersions := Nil
addCommandAlias(
  "native-image",
  "; gitmini/graalvm-native-image:packageBin ; taskready"
)
commands += Command.command("taskready") { s =>
  import scala.sys.process._
  scala.util.Try("say 'native-image ready'".!)
  s
}

lazy val gitmini = project
  .settings(
    moduleName := "git-mini",
    mainClass := Some("gitmini.GitMini"),
    scalaVersion := "2.13.2",
    libraryDependencies ++= List(
      "com.geirsson" %% "metaconfig-core" % "0.9.10",
      "org.scalameta" %% "munit" % "0.7.7" % Test
    ),
    testFrameworks := List(new TestFramework("munit.Framework")),
    buildInfoPackage := "gitmini.internal",
    buildInfoKeys := Seq[BuildInfoKey](
      version
    ),
    graalVMNativeImageOptions ++= {
      val reflectionFile =
        Keys.sourceDirectory.in(Compile).value./("graal")./("reflection.json")
      assert(reflectionFile.exists, "no such file: " + reflectionFile)
      List(
        "-H:+ReportUnsupportedElementsAtRuntime",
        "-H:+RemoveSaturatedTypeFlows",
        s"-H:ReflectionConfigurationFiles=$reflectionFile",
        "-H:+ReportExceptionStackTraces",
        "-H:EnableURLProtocols=http,https",
        "--enable-http",
        "--enable-https",
        "--enable-all-security-services",
        "--install-exit-handlers",
        "--initialize-at-build-time",
        // "--initialize-at-build-time=scala.runtime.Statics$VM",
        // "--initialize-at-build-time=scala.Symbol",
        // "--initialize-at-build-time=scala.Function1",
        // "--initialize-at-build-time=scala.Function2",
        // "--initialize-at-build-time=scala.runtime.StructuralCallSite",
        // "--initialize-at-build-time=scala.runtime.EmptyMethodCache",
        "--no-server",
        "--no-fallback",
        "--allow-incomplete-classpath",
      )
    }
  )
  .enablePlugins(BuildInfoPlugin, GraalVMNativeImagePlugin)
