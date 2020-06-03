package gitmini

import java.io.File
import scala.io.StdIn

class CreateCommandSuite extends BaseSuite {
  val hello = "hello/src/Hello.scala"
  val goodbye = "goodbye/src/Goodbye.scala"
  test("install") {
    init(
      s"""
         |/$hello
         |package hello
         |object Hello
         |/$goodbye
         |package goodbye
         |object Goodbye
         |""".stripMargin
    )
    run(CreateCommand.name, "--name", "hello", "hello/src")
    assertEquals(lsFiles(), List(hello))
    assertEquals(
      execString("git", "diff", "origin/master"),
      ""
    )
    run("uninstall")
    assertEquals(lsFiles(), List(goodbye, hello))
  }

}
