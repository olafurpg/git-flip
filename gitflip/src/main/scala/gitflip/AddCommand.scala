package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import org.typelevel.paiges.Doc

object AddCommand extends Command[AddOptions]("add") {
  override def description: Doc =
    Doc.text("Add changed files to git from minirepo")
  def run(value: Value, app: CliApp): Int = {
    SharedCommand.currentName(app) match {
      case Some(name) =>
        val includes = app.includes(name)
        if (!Files.isRegularFile(includes)) {
          app.error(s"no such file: $includes")
          1
        } else {
          val dirs = includes.readText.linesIterator.toList
          app.exec(List("git", "add", "--force") ++ dirs)
        }
      case None =>
        1
    }
  }

}
