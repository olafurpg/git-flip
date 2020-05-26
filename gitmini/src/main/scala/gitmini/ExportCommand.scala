package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._
import java.nio.file.Files

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
    for {
      _ <-
        if (diff.isEmpty) {
          app.error(
            "nothing to export because the diff to origin/master is empty. " +
              "To fix this problem, commit some changes before running export."
          )
          1
        } else {
          0
        }
      _ <- app.exec(
        List(
          "git",
          "format-patch",
          "--full-index",
          "--output-directory",
          out.toString,
          "origin/master..HEAD"
        )
      )
      _ <- SwitchCommand.run(SwitchOptions(List(app.megarepoName)), app.cli)
      _ <- app.exec(List("git", "checkout", "--") ++ diff)
      _ <- app.checkoutMegarepoBranch()
      _ <- app.exec("git", "am", out.toString())
      _ <- SwitchCommand.run(SwitchOptions(List(minirepoName)), app.cli)
    } yield 0
  }
}
