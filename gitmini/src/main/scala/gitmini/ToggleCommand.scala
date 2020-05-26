package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files

object ToggleCommand extends Command[Unit]("toggle") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    if (Files.isRegularFile(app.pausefile)) {
      PlayCommand.run((), cli)
    } else {
      PauseCommand.run((), cli)
    }
  }
}
