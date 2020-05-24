package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._
import java.nio.file.Files
import scala.jdk.CollectionConverters._
import org.typelevel.paiges.Doc

object InfoCommand extends Command[InfoOptions]("info") {
  override def description: Doc =
    Doc.text("Show directories that are tracked by this mini-repo")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    val minirepo = value.minirepo match {
      case Nil => app.currentName().toList
      case els => els
    }
    val toplevel = app.toplevel
    for {
      repo <- minirepo
      include <- app.readIncludes(repo, printWarnings = false)
    } {
      app.cli.out.println(toplevel.relativize(include))
    }
    0
  }
}
