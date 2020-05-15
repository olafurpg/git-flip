package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

object AddCommand extends Command[AddOptions]("add") {
  def run(value: Value, app: CliApp): Int = {
    currentName(app) match {
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

  def currentName(app: CliApp): Option[String] = {
    if (!Files.isRegularFile(app.git)) {
      app.error(s"cannot add since ${app.git} is not a regular file")
      None
    } else {
      app.git.readText.linesIterator.toList.collectFirst {
        case s"gitdir: $path" => Paths.get(path).getFileName().toString()
      }
    }
  }
}
