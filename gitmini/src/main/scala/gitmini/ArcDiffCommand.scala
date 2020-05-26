package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._

object ArcDiffCommand extends Command[ArcDiffOptions]("arc-diff") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case None =>
        1
      case Some(app.megarepoName) =>
        app.error(
          "can only run arc-diff when inside a minirepo. " +
            "To fix this problem, 'arc diff' directly since you " +
            "are already inside the megarepo"
        )
        1
      case Some(minirepoName) =>
        val minirepoBranch = app.currentBranch()
        for {
          _ <- ExportCommand.run(ExportOptions(), cli)
          _ <- SwitchCommand.run(SwitchOptions(List(app.megarepoName)), cli)
          _ <- {
            val exit = app.execTty(
              List("arc", "diff") ++ value.arguments,
              isSilent = false
            )
            for {
              _ <- SwitchCommand.run(SwitchOptions(List(minirepoName)), cli)
            } yield exit
          }
        } yield 0
    }
  }
}
