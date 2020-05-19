package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import gitflip.GitflipEnrichments._
import scala.jdk.CollectionConverters._
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import org.typelevel.paiges.Doc
import scala.collection.mutable
import java.nio.file.Paths
import scala.util.Try
import scala.io.StdIn

object StartCommand extends Command[StartOptions]("start") {
  override def description: Doc = Doc.text("Create a new mini-repo")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    if (value.directories.isEmpty) {
      app.cli.error(
        s"can't create a new mini-repo from an empty list of directories to exclude. " +
          s"To fix this problem, pass the directory that you wish to exclude. For example:\n\t" +
          s"${app.cli.binaryName} create my-directory"
      )
      1
    } else if (!Files.exists(app.cli.workingDirectory)) {
      app.error(
        s"working directory does not exist: ${app.cli.workingDirectory}"
      )
      1
    } else if (Try(app.toplevel).isFailure) {
      app.cli.error(
        s"${app.cli.binaryName} can only run inside a git repository"
      )
      1
    } else if (value.name.isEmpty) {
      app.cli.error(
        "missing --name. To fix this problem, provide a name for the new mini-repo:\n\t" +
          s"${app.cli.binaryName} create --name mini-repo_NAME ${value.directories.mkString(" ")}"
      )
      1
    } else if (Files.isDirectory(app.minirepo(value.name))) {
      app.cli.error(
        s"can't create mini-repo '$name' because it already exists.\n\t" +
          s"To amend this mini-repo run: ${app.cli.binaryName} amend $name"
      )
      1
    } else if (app.requireCleanStatus()) {
      1
    } else if (
      Files.isRegularFile(app.git) &&
      app.requireInsideMegarepo(name)
    ) {
      pprint.log(app.currentName())
      ???
      1
    } else if (app.requireBranch("master", name)) {
      ???
      1
    } else if (app.requireMasterBranchIsUpToDate(name)) {
      ???
      1
    } else if (InstallCommand.run((), app, isInstallCommand = false) != 0) {
      app.error(s"${app.binaryName} installation failed")
      1
    } else {
      require(Files.isRegularFile(app.git), "git-flip is not installed")
      Files.deleteIfExists(app.git)
      val exit = for {
        _ <- app.exec(
          "git",
          "init",
          "--separate-git-dir",
          app.minirepo(value.name).toString()
        )
        _ <- {
          writeInclude(value, app, value.name)
          writeExclude(app, value.name)
          app.commitAll(app.syncToMegarepoMessage())
        }
        _ <- app.exec("git", "branch", "origin/master", "master")
      } yield {
        app.info(onSuccessMessage(app))
        0
      }

      if (exit != 0) {
        app.cli.error(s"Failed to create new mini-repo named '$name'")
      }
      exit
    }
  }

  def onSuccessMessage(app: Flip): String = {
    val examples = Doc.tabulate(
      ' ',
      " # ",
      List[(String, Doc)](
        "git status" -> Doc.text(
          "see if you have uncommited changes"
        ),
        "git ls-files" -> Doc.text(
          "list what files are tracked in this mini-repo"
        ),
        "git flip sync" -> Doc.text(
          "pull latest changes from remote into this mini-repo"
        ),
        s"git flip switch ${app.megarepoName}" -> Doc.text(
          "pull latest changes from remote into this mini-repo"
        )
      )
    )
    "successfully initialized a new mini-repo. " +
      "Here are some example commands you may want to run:\n" +
      examples.indent(2).render(80)
  }
  def writeInclude(value: Value, app: Flip, name: String): Unit = {
    val includes = app.includes(name)
    val toplevel = app.toplevel
    val a = toplevel
    val includeDirectories = value.directories.map { dir =>
      val absolutePath = app.toAbsolutePath(dir)
      val b = absolutePath
      val relativePath = toplevel.relativize(absolutePath)
      relativePath.toString
    }
    Files.write(
      includes,
      includeDirectories.asJava,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
  }
  def writeExclude(app: Flip, name: String): Unit = {
    val includes = app.readIncludes(name, printWarnings = true)
    val excludes = mutable.ListBuffer.empty[String]
    val toplevel = app.toplevel
    excludes += "/*"
    includes.foreach { include =>
      val relativePath = toplevel.relativize(include)
      excludes += s"!/$relativePath"
    }
    Files.write(
      app.exclude(name),
      excludes.asJava,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
  }
}
