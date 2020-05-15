package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Files
import scala.jdk.CollectionConverters._
import org.typelevel.paiges.Doc

object InfoCommand extends Command[InfoOptions]("info") {
  override def description: Doc =
    Doc.text("Show directories that tracked in this minirepo")
  def run(value: Value, app: CliApp): Int = {
    val minirepo = value.minirepo match {
      case Nil => AddCommand.currentName(app) :: Nil
      case els => els
    }
    val errors: List[Int] = value.minirepo.map { repo =>
      val exclude = app.exclude(repo)
      if (Files.isRegularFile(exclude)) {
        Files.readAllLines(exclude).forEach { line =>
          if (line.startsWith("#") || line.isEmpty()) ()
          else app.out.println(line)
        }
        0
      } else {
        app.error(s"no such file '$exclude'")
        1
      }
    }
    errors.sum
  }
}
