package gitmini

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import GitMiniEnrichments._
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import org.typelevel.paiges.Doc
import metaconfig.cli.{TabCompletionContext, TabCompletionItem}
import metaconfig.internal.Levenshtein

object SwitchCommand extends Command[SwitchOptions]("switch") {
  override def description: Doc = Doc.text("Switch between git repos")
  override def complete(
      context: TabCompletionContext
  ): List[TabCompletionItem] = {
    ListCommand.all(new Flip(context.app)).map(i => TabCompletionItem(i))
  }
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.withMinirepo("switch", value.minirepo) { newName =>
      val minirepo = app.minirepo(newName)
      app.currentName() match {
        case None =>
          1
        case Some(oldName) =>
          if (newName == oldName) {
            app.info(s"nothing to do, already on '$oldName'")
            0
          } else {
            app.pausefile.writeText(oldName)
            app.git.writeText(s"gitdir: $minirepo")
            if (newName == app.megarepoName) {
              app.cli.info(s"switched to the megarepo")
            } else {
              app.cli.info(s"switched to the minirepo '$newName'")
            }
          }
      }
      0
    }
  }
}
