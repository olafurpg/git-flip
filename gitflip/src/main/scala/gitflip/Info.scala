package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Files
import scala.jdk.CollectionConverters._

object InfoCommand extends Command[InfoOptions]("info") {
  def run(value: Value, app: CliApp): Int = {
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
