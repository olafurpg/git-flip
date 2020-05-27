package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._
import java.nio.file.Files
import scala.collection.mutable

object ExportCommand extends Command[ExportOptions]("export") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case None =>
        1
      case Some(app.megarepoName) =>
        app.error(
          s"can only run ${app.binaryName} $name from minirepo. " +
            s"Since you are already inside the megarepo, run 'git commit -am' directly instead."
        )
        1
      case Some(minirepoName) =>
        if (app.isDirtyStatus("export")) {
          1
        } else {
          run(app, minirepoName)
        }
    }
  }
  def run(app: Flip, minirepoName: String): Int = {
    val out = Files.createTempDirectory("git-mini")
    val diff = app
      .execString(List("git", "diff", "--name-only", "origin/master"))
      .linesIterator
      .toList
    val minirepoBranch = app.currentBranch()
    val megarepoBranch = app.flipBranchName(minirepoName, minirepoBranch)
    val megarepoSha = app.megarepoSha()
    val baseRef = "origin/master"
    for {
      _ <- {
        if (diff.isEmpty) {
          app.error(
            "nothing to export because the diff to origin/master is empty. " +
              "To fix this problem, commit some changes before running export."
          )
          1
        } else {
          0
        }
      }
      _ <- {
        val diffCommand =
          List(
            "git",
            "diff",
            megarepoSha
          ) ++ app.readIncludes(minirepoName).map(_.toString)
        val diff = app.proc(
          diffCommand,
          env = Map(
            "GIT_ALTERNATE_OBJECT_DIRECTORIES" ->
              app.megarepo.resolve("objects").toString()
          )
        )
        val applyCommand = List(
          "git",
          s"--git-dir=${app.megarepo}",
          "apply",
          "--index",
          "--cached"
        )
        val apply = app.proc(
          applyCommand,
          env = Map.empty
        )
        (diff #> apply).!
      }
      _ <- {
        if (megarepoBranch == app.megarepoBranch()) {
          0
        } else {
          for {
            _ <- app.exec(
              "git",
              s"--git-dir=${app.megarepo}",
              "branch",
              "-f",
              megarepoBranch,
              "HEAD"
            )
            _ <- app.exec("git", "symbolic-ref", "HEAD", megarepoBranch)
          } yield 0
        }
      }
      _ <- {
        val commitMessage =
          Files.createTempFile(app.binaryName, "COMMIT_EDIT_MSG")
        val template = app.execString(
          List("git", "log", "--pretty", "- %B", s"$baseRef..HEAD")
        )
        commitMessage.writeText(template)
        app.execTty(
          List(
            "git",
            s"--git-dir=${app.megarepo}",
            "commit",
            "--template",
            commitMessage.toString()
          ),
          isSilent = true
        )
      }
    } yield 0
  }
}
