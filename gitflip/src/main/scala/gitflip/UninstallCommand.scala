package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitFlipEnrichments._
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.Path
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import org.typelevel.paiges.Doc
import java.nio.file.StandardCopyOption

object UninstallCommand extends Command[Unit]("uninstall") {
  override def description: Doc = Doc.text("Revert git-flip installation")
  def run(value: Value, app: CliApp): Int = {
    if (!Files.isRegularFile(app.git)) {
      app.info(s"nothing to do, ${app.binaryName} is not installed")
      0
    } else {
      val message =
        s"are you sure you want to uninstall ${app.binaryName}? [y/N] "
      app.confirm(message) match {
        case Left(exit) => exit
        case Right(isConfirmed) =>
          if (isConfirmed) {
            val megarepo = app.megarepo
            val git = app.git
            val gitflip = app.gitflip
            app.info(s"rm $git")
            Files.delete(app.git)
            app.info(s"mv $megarepo $git")
            Files.move(megarepo, git)
            app.info(s"rm -rf $gitflip")
            DeleteVisitor.deleteRecursively(gitflip, app)
            app.info(
              s"uninstalled ${app.binaryName}, this git repository should be reverted back to normal now."
            )
          } else {
            app.info("aborting uninstall")
          }
      }
      0
    }
  }
}
