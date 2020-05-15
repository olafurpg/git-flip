package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import GitFlipEnrichments._
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import org.typelevel.paiges.Doc

object SwitchCommand extends Command[SwitchOptions]("switch") {
  override def description: Doc = Doc.text("Switch between git minirepos")
  def run(value: Value, app: CliApp): Int = {
    withMinirepo("switch", value.minirepo, app) { name =>
      val minirepo = app.minirepo(name)
      // TODO: handle the case when the current repo is already this project.
      Files.write(
        app.git,
        s"gitdir: $minirepo".getBytes(StandardCharsets.UTF_8)
      )
      app.info(s"switched to minirepo '$name'")
      0
    }
  }
  def withMinirepo(what: String, minirepos: List[String], app: CliApp)(
      operation: String => Int
  ): Int = {
    minirepos.headOption match {
      case None =>
        app.error(
          s"can't $what to a minirepo since no argument was provided.\n\t" +
            s"To list existing minirepos run: ${app.binaryName} list"
        )
        1
      case Some(name) =>
        val minirepo = app.minirepo(name)
        if (!Files.isRegularFile(app.git)) {
          app.error(
            s"not a regular file '${app.git}'. Did you initialize this repo for git-flip?\n\t" +
              s"To initialize this repo for git-flip run: ${app.binaryName} init"
          )
          1
        } else if (!Files.isDirectory(minirepo)) {
          app.error(
            s"minirepo '${app.git}' does not exist. Did you create this minirepo?\n\t" +
              s"To create this minirepo: ${app.binaryName} create --name $name <subdirectory>..."
          )
          1
        } else {
          operation(name)
        }
    }

  }
}
