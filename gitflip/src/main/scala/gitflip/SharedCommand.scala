package gitflip

import java.nio.file.Files
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Paths
import scala.collection.immutable.Nil

object SharedCommand {
  def withCurrentName(minirepo: List[String], app: CliApp)(
      fn: String => Int
  ): Int = {
    minirepo match {
      case Nil =>
        currentName(app) match {
          case Some(name) =>
            fn(name)
          case None =>
            1
        }
      case name :: Nil =>
        fn(name)
      case many =>
        app.error(
          s"ambiguous minirepo. " +
            s"Expected one argument but obtained $many. " +
            s"To fix this problem provide only one argument."
        )
        1
    }
  }
  def currentName(app: CliApp): Option[String] = {
    if (!Files.isRegularFile(app.git)) {
      app.error(s"${app.binaryName} is not installed")
      None
    } else {
      app.git.readText.linesIterator.toList.collectFirst {
        case s"gitdir: $path" => Paths.get(path).getFileName().toString()
      }
    }
  }

}
