package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import org.typelevel.paiges.Doc

object StopCommand extends Command[Unit]("stop") {
  override def description: Doc =
    Doc.text("Delete this minirepo and go back to the megarepo")
  def run(value: Value, app: CliApp): Int = ???
}
