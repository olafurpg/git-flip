package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import GitMiniEnrichments._

object PlayCommand extends Command[Unit]("play") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case None => 1
      case Some(app.megarepoName) =>
        if (!Files.isRegularFile(app.pausefile)) {
          app.error(
            "can't run play command because you need to run pause first."
          )
          1
        } else {
          val minirepo = app.pausefile.readText
          Files.deleteIfExists(app.pausefile)
          SwitchCommand.run(SwitchOptions(List(minirepo)), cli)
        }
      case Some(other) =>
        app.info(s"nothing to do, already in minirepo '$other'")
        0
    }
  }
}
