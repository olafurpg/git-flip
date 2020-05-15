package gitflip

import scala.jdk.CollectionConverters._
import metaconfig.cli.CliApp
import metaconfig.cli.HelpCommand
import metaconfig.cli.VersionCommand
import metaconfig.cli.TabCompleteCommand
import metaconfig.cli.Command
import java.nio.file.Files
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import gitflip.GitFlipEnrichments._
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object InitCommand extends Command[Unit]("init") {
  def run(value: Value, app: CliApp): Int = {
    if (Files.isRegularFile(app.git)) 0
    else if (Files.isDirectory(app.git)) {
      app.warn(
        s"git-flip is an experimental tool that may destroy your git repository."
      )
      app.warn(
        s"Make sure you have prepared a backup of this git repository before continuing."
      )
      val message = s"Initialize git-flip for '${app.workingDirectory}'? [y/N] "
      Try(app.readLine(message)) match {
        case Success("y" | "Y" | "Yes" | "yes") =>
          initialize(app)
        case Success("n" | "N" | "No" | "no") =>
          app.info("doing nothing")
          0
        case Success(other) =>
          app.error(
            s"unknown response '$other'. Expected 'y' for test or 'n' for no."
          )
          1
        case Failure(exception) =>
          exception.printStackTrace(app.err)
          1
      }
    } else if (!Files.exists(app.git)) {
      app.error(
        s"Not a git repository '${app.workingDirectory}'. " +
          s"To fix this problem, make sure the path '${app.git}' points to a git directory or file."
      )
      1
    } else if (!Files.isSymbolicLink(app.git)) {
      app.error(s"symbolic links are not supported: ${app.git}")
      1
    } else {
      app.error(s"unsupported file type: ${app.git}")
      1
    }
  }
  def initialize(app: CliApp): Int = {
    import scala.sys.process._
    Files.createDirectories(app.megarepo)
    if (
      app.exec(
        "git",
        "init",
        "--separate-git-dir",
        app.megarepo.toString()
      ) == 0
    ) {
      commitGitignore(app)
    } else {
      1
    }
  }

  def commitGitignore(app: CliApp): Int = {
    if (app.exec("git", "check-ignore", app.gitflipName) == 1) {
      if (
        Files.isRegularFile(app.gitignore) ||
        !Files.exists(app.gitignore)
      ) {
        Files.write(
          app.gitignore,
          List(app.gitflipName).asJava,
          StandardOpenOption.APPEND,
          StandardOpenOption.CREATE
        )
        if (app.exec("git", "add", app.gitignore.getFileName().toString) == 0) {
          app.exec("git", "commit", "-m", s"Initialize ${app.gitflipName}")
        } else {
          1
        }
      } else {
        app.warn(
          s"unable to automatically gitignore the path '${app.gitflipName}'. " +
            s"To fix this warning, manually update .gitignore to exclude '${app.gitflipName}'."
        )
        0
      }
    } else {
      1
    }
  }
}
