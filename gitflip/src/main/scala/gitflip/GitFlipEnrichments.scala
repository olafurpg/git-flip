package gitflip

import metaconfig.cli.CliApp
import java.nio.file.Path
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import fansi.Color
import java.nio.file.Paths
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object GitFlipEnrichments {

  def continueIfSuccessful(exit: Int)(thunk: => Int): Int = {
    if (exit == 0) thunk
    else exit
  }
  implicit class XtensionExitCode(exit: Int) {
    def ifSuccessful(thunk: => Int): Int = {
      if (exit == 0) thunk
      else exit
    }
  }
  implicit class XtensionPath(path: Path) {
    def listForeach(fn: Path => Unit): Unit = {
      val ls = Files.list(path)
      try ls.forEach(p => fn(p))
      finally ls.close()
    }
    def readText: String =
      new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }
  implicit class XtensionCliApp(app: CliApp) {
    lazy val toplevel: Path =
      Paths.get(
        app.execString(List("git", "rev-parse", "--show-toplevel")).trim
      )
    def toAbsolutePath(path: Path): Path = {
      if (path.isAbsolute()) path
      else app.workingDirectory.resolve(path)
    }
    def isGitRepository: Boolean =
      try {
        toplevel
        true
      } catch {
        case NonFatal(_) =>
          false
      }
    def git: Path = toplevel.resolve(".git")
    def gitignore: Path = toplevel.resolve(".gitignore")
    def gitflip: Path =
      toplevel
        .resolve("..")
        .resolve(s"git-flips")
        .resolve(toplevel.getFileName())
        .normalize()
    def megarepo: Path = gitflip.resolve("megarepo")
    def readLine(message: String): String = {
      app.err.print(message)
      new BufferedReader(new InputStreamReader(app.in)).readLine()
    }
    def minirepo(name: String): Path = {
      gitflip.resolve(name)
    }
    def exclude(name: String): Path = {
      minirepo(name).resolve("info").resolve("exclude")
    }
    def includes(name: String): Path = {
      minirepo(name).resolve("info").resolve("includes")
    }
    def readIncludes(name: String): Set[Path] = {
      import scala.jdk.CollectionConverters._
      val i = includes(name)
      if (Files.isRegularFile(i)) {
        val t = toplevel
        val paths = Set.newBuilder[Path]
        val lines = Files.lines(i)
        try lines.forEach(line => paths += Paths.get(line))
        finally lines.close()
        paths.result()
      } else {
        Set.empty
      }
    }
    def exec(
        command: List[String],
        logger: ProcessLogger = ProcessLogger(out => app.err.println(out))
    ): Int = {
      scala.sys.process
        .Process(command, cwd = Some(app.workingDirectory.toFile()))
        .!(logger)
    }
    def exec(command: String*): Int = {
      exec(command.toList)
    }
    def execString(command: List[String]): String = {
      scala.sys.process
        .Process(command, cwd = Some(app.workingDirectory.toFile()))
        .!!
    }
    def confirm(message: String): Either[Int, Boolean] =
      Try(
        app.readLine((Color.LightYellow("warn: ") ++ message).toString)
      ) match {
        case Success("y" | "Y" | "Yes" | "yes") =>
          Right(true)
        case Success("n" | "N" | "No" | "no" | null) =>
          Right(false)
        case Success(other) =>
          app.error(
            s"unknown response '$other'. Expected 'y' for test or 'n' for no."
          )
          Left(1)
        case Failure(exception) =>
          exception.printStackTrace(app.err)
          Left(1)
      }
  }

}
