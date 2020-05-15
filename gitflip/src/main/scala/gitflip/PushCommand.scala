package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import org.typelevel.paiges.Doc

object PushCommand extends Command[Unit]("push") {
  override def description: Doc = Doc.text("Push minirepo to remote origin")
  def run(value: Value, app: CliApp): Int = {
    0
  }
}
