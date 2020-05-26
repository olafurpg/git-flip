package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._

object ArcDiffCommand extends Command[ArcDiffOptions]("arc-diff") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    for {
      _ <- ExportCommand.run(ExportOptions(), cli)
      _ <- app.execTty(
        List("arc", "diff") ++ value.arguments,
        isSilent = false,
        environment = Map(
          "GIT_DIR" -> app.megarepo.toString()
        )
      )
    } yield 0
  }
}
