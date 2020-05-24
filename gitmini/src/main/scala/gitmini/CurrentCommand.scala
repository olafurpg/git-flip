package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import org.typelevel.paiges.Doc

object CurrentCommand extends Command[Unit]("current") {
  override def description: Doc =
    Doc.text("Show the current active minirepo")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case Some(name) =>
        app.cli.out.println(name)
        0
      case None =>
        1
    }
  }
}
