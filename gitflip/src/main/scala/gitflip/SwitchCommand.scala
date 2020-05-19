package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import GitflipEnrichments._
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
    app.withMinirepo("switch", value.minirepo) { name =>
      val minirepo = app.minirepo(name)
      // TODO: handle the case when the current repo is already this project.
      Files.write(
        app.git,
        s"gitdir: $minirepo".getBytes(StandardCharsets.UTF_8)
      )
      app.cli.info(s"switched to mini-repo '$name'")
      0
    }
  }
}
