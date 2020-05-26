package gitmini

import metaconfig.cli.CliApp
import metaconfig.cli.Command
import GitMiniEnrichments._
import java.io.PrintWriter
import java.nio.file.Path
import scala.util.control.NonFatal
import org.typelevel.paiges.Doc

object AmendCommand extends Command[AmendOptions]("amend") {
  override def description: Doc =
    Doc.text("Edit the list of directories that are tracked by this minirepo")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.withMinirepo("amend", value.minirepo) { name =>
      if (app.megarepo == app.minirepo(name)) {
        app.cli.error(
          "can't amend the mega-repo. To run amend, you must be inside a minirepo."
        )
        1
      } else {
        Option(System.getenv("EDITOR")) match {
          case None =>
            app.cli.error(
              "can't amend since the environment variable $EDITOR is not defined.\n\t" +
                s"To fix this problem run: EDITOR=vim ${app.cli.arguments.mkString(" ")}"
            )
            1
          case Some(editor) =>
            app
              .execTty(
                List(editor, app.includes(name).toString()),
                isSilent = true
              )
              .ifSuccessful {
                StartCommand.writeExclude(app, name)
                app.commitAll(s"${app.binaryName}: amend minirepo")
              }
        }
      }
    }
  }

}
