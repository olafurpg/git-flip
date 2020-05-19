package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import org.typelevel.paiges.Doc

object StopCommand extends Command[Unit]("stop") {
  override def description: Doc =
    Doc.text("Delete this mini-repo and go back to the mega-repo")
  def run(value: Value, app: CliApp): Int = ???
}
