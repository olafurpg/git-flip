package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import GitFlipEnrichments._

object ListCommand extends Command[Unit]("list") {
  def run(value: Value, app: CliApp): Int = {
    val ls = Files.list(app.gitflip)
    try {
      ls.forEach { p => app.out.println(p.getFileName()) }
    } finally ls.close()
    0
  }
}
