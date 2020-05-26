package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._

object PauseCommand extends Command[Unit]("pause") {
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case Some(app.megarepoName) =>
        app.error("nothing to do because you are already inside the megarepo")
        0
      case Some(name) =>
        app.pausefile.writeText(name)
        SwitchCommand.run(SwitchOptions(List(app.megarepoName)), cli)
      case None =>
        1
    }
  }
}
