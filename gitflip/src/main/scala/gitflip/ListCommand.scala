package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import GitFlipEnrichments._
import org.typelevel.paiges.Doc

object ListCommand extends Command[Unit]("list") {
  override def description: Doc =
    Doc.text("List all minirepos in this git workspace")
  def run(value: Value, app: CliApp): Int = {
    val ls = Files.list(app.gitflip)
    try {
      ls.forEach { p => app.out.println(p.getFileName()) }
    } finally ls.close()
    0
  }
}
