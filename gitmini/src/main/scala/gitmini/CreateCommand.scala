package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import gitmini.GitMiniEnrichments._
import scala.jdk.CollectionConverters._
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import org.typelevel.paiges.Doc
import scala.collection.mutable
import java.nio.file.Paths
import scala.util.Try
import scala.io.StdIn

object CreateCommand extends Command[CreateOptions]("create") {
  override def description: Doc = Doc.text("Create a new minirepo")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    val remoteName = app.remoteName(value.minirepoName)
    if (value.directories.isEmpty) {
      app.cli.error(
        s"can't create a new minirepo from an empty list of directories to exclude. " +
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
    } else if (value.isMissingExplicitName) {
      app.cli.error(
        "missing --name. To fix this problem, provide a name for the new minirepo:\n\t" +
          s"${app.cli.binaryName} create --name minirepo_NAME ${value.directories.mkString(" ")}"
      )
      1
    } else if (Files.isDirectory(app.minirepo(value.minirepoName))) {
      app.cli.error(
        s"can't create minirepo '$name' because it already exists.\n\t" +
          s"To amend this minirepo run: ${app.cli.binaryName} amend $name"
      )
      1
    } else if (app.isDirtyStatus("create a minirepo")) {
      1
    } else if (
      Files.isRegularFile(app.git) &&
      app.requireInsideMegarepo(name)
    ) {
      1
    } else if (app.requireBranch("master", name)) {
      1
    } else if (app.requireMasterBranchIsUpToDate(name)) {
      1
    } else if (app.remote().contains(remoteName)) {
      app.error(
        s"can't create minirepo with the name '${value.minirepoName}' because " +
          s"there is already another remote with a conflicting name. " +
          s"To fix this problem, either remove the remote with 'git remote remove " +
          s"$remoteName' or use a different name with '${app.binaryName} ${name} --name OTHER_NAME'."
      )
      1
    } else if (InstallCommand.run((), app, isInstallCommand = false) != 0) {
      app.error(s"${app.binaryName} installation failed")
      1
    } else {
      require(Files.isRegularFile(app.git), "git-mini is not installed")
      Files.deleteIfExists(app.git)
      val minirepo = app.minirepo(value.minirepoName).toString()
      val exit = for {
        _ <- app.exec(
          "git",
          "init",
          "--separate-git-dir",
          minirepo
        )
        _ <- {
          writeInclude(value, app, value.minirepoName)
          writeExclude(app, value.minirepoName)
          app.commitAll(app.syncToMegarepoMessage())
        }
        _ <- app.exec("git", "branch", "origin/master", "master")
        _ <- {
          val config = scala.util.Try {
            app
              .execString(
                List(
                  "git",
                  s"--git-dir=${app.megarepo}",
                  "config",
                  "--get",
                  "remote.origin.url"
                ),
                isSilent = true
              )
              .trim()
          }.toOption
          config match {
            case Some(remote) =>
              // NOTE(olafur): we set the origin remote to prevent errors in tool
              // that assume there exists a remote.
              app.exec("git", "remote", "add", "origin", remote)
            case None =>
              0
          }
        }
      } yield {
        app.info(onSuccessMessage(app))
        0
      }

      if (exit != 0) {
        app.cli.error(s"Failed to create new minirepo named '$name'")
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
          "list what files are tracked in this minirepo"
        ),
        "git mini sync" -> Doc.text(
          "pull latest changes from remote into this minirepo"
        ),
        s"git mini switch ${app.megarepoName}" -> Doc.text(
          "pull latest changes from remote into this minirepo"
        )
      )
    )
    "successfully initialized a new minirepo. " +
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
    val includes = app.readIncludes(name)
    val excludes = mutable.LinkedHashSet.empty[String]
    val toplevel = app.toplevel
    includes.foreach { include =>
      val relativePath = toplevel.relativize(include)
      1.to(relativePath.getNameCount()).foreach { i =>
        val subpath = relativePath.subpath(0, i)
        excludes += subpath
          .resolveSibling("*")
          .iterator()
          .asScala
          .mkString("/", "/", "")
        excludes += s"!/$subpath"
      }
    }
    Files.write(
      app.exclude(name),
      excludes.asJava,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
  }
}
