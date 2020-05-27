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
    val startpoint = "origin/master"
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
        pprint.log(diffCommand)
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
        pprint.log(applyCommand)
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
      _ <- app.execTty(
        List("git", s"--git-dir=${app.megarepo}", "commit"),
        isSilent = false
      )
      _ <-
        if (app.status().nonEmpty) {
          app.error(
            s"the files below have untracked changes:\n${app.status().mkString("\n")}"
          )
          1
        } else 0
      _ <- app.exec(List("git", "reset", "--hard", "origin/master"))
      _ <- {
        val patches = mutable.ListBuffer.empty[String]
        out.listForeach(p => patches += p.toString())
        app.exec(List("git", "am") ++ patches.sorted)
      }
      _ <- app.exec("git", "checkout", megarepoBranch)
      _ <- SwitchCommand.run(SwitchOptions(List(minirepoName)), app.cli)
    } yield 0
  }
}
