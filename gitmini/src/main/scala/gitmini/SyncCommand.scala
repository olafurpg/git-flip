package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import GitMiniEnrichments._
import java.nio.file.Files
import org.typelevel.paiges.Doc
import metaconfig.cli.Messages

object SyncCommand extends Command[SyncOptions]("sync") {
  override def description: Doc =
    Doc.text("Pull latest changes from the megarepo into the minirepo")
  override def options: Doc = Messages.options(SyncOptions.default)
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    app.currentName() match {
      case None => 1
      case Some(name) =>
        val minirepo = app.minirepo(name)
        if (Files.isSameFile(app.megarepo, minirepo)) {
          app.error(
            s"can't ${app.binaryName} sync inside the megarepo, use 'git pull origin master' instead"
          )
          1
        } else if (app.megarepoBranch() != "master") {
          app.error(
            s"can't ${app.binaryName} sync when the megarepo is on branch '${app.megarepoBranch}'. " +
              s"To fix this problem, manually change to the 'master' branch in the megarepo:" +
              s"\n\t${app.binaryName} switch ${app.megarepoName} && \\" +
              s"\n\tgit checkout master &&\\" +
              s"\n\t${app.binaryName} switch $name"
          )
          1
        } else if (app.isDirtyStatus("sync")) {
          1
        } else {
          val originalBranch = app.minirepoBranch()
          for {
            _ <- app.checkoutOriginMaster()
            _ <- app.pullOriginMasterInMegarepo()
            _ <- app.commitAll(app.syncToMegarepoMessage())
            _ <- app.exec("git", "checkout", originalBranch)
            _ <- app.exec("git", "rebase", "origin/master")
          } yield 0
        }
    }
  }
}
