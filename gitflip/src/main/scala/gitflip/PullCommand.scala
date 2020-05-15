package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import org.typelevel.paiges.Doc

object PullCommand extends Command[Unit]("pull") {
  override def description: Doc =
    Doc.text("Pull latest changes from remote origin")
  def run(value: Value, app: CliApp): Int = {
    0
  }
}
