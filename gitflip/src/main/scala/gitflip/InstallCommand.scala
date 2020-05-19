package gitflip

import scala.jdk.CollectionConverters._
import metaconfig.cli.CliApp
import metaconfig.cli.HelpCommand
import metaconfig.cli.VersionCommand
import metaconfig.cli.TabCompleteCommand
import metaconfig.cli.Command
import java.nio.file.Files
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import gitflip.GitflipEnrichments._
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import org.typelevel.paiges.Doc
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.io.StdIn

object InstallCommand extends Command[Unit]("install") {
  override def description: Doc =
    Doc.text("Configure this git repository for git-flip")
  def run(value: Value, cli: CliApp): Int = {
    run(value, new Flip(cli), isInstallCommand = true)
  }
  def run(value: Value, app: Flip, isInstallCommand: Boolean): Int = {
    if (Files.isRegularFile(app.git)) {
      if (isInstallCommand) {
        app.cli.info(
          s"nothing to do, ${app.cli.binaryName} is already installed"
        )
      }
      0
    } else if (Files.isDirectory(app.git)) {
      app.cli.warn(
        s"${app.cli.binaryName} is an experimental tool that may destroy your git repository."
      )
      app.cli.warn(
        s"make sure you have prepared a backup of the git repository '${app.git}' before continuing."
      )
      val message =
        s"are you sure you want to install ${app.cli.binaryName}? [y/N] "
      app.confirm(message) match {
        case Left(exit) => exit
        case Right(isConfirmed) =>
          if (isConfirmed) {
            val exit = initialize(app)
            if (exit == 0 && isInstallCommand) {
              app.cli.info(
                s"installation complete. To get started with run:\n\t" +
                  s"${app.cli.binaryName} create --name NAME <directory>... "
              )
            }
            exit
          } else {
            app.cli.info("doing nothing")
            1
          }
      }
    } else if (!Files.exists(app.git)) {
      app.cli.error(
        s"not a git repository '${app.cli.workingDirectory}'. " +
          s"to fix this problem, make sure the path '${app.git}' points to a git directory or file."
      )
      1
    } else if (!Files.isSymbolicLink(app.git)) {
      app.cli.error(s"symbolic links are not supported: ${app.git}")
      1
    } else {
      app.cli.error(s"unsupported file type: ${app.git}")
      1
    }
  }
  def initialize(app: Flip): Int = {
    import scala.sys.process._
    Files.createDirectories(app.megarepo)
    val args =
      List[String]("git", "init", "--separate-git-dir", app.megarepo.toString())
    val baos = new ByteArrayOutputStream()
    val stdout = new PrintStream(baos)
    val exit =
      app.exec(args, logger = ProcessLogger(out => stdout.println(out)))
    val out = baos.toString()
    if (
      exit == 128 &&
      out.startsWith("fatal: cannot copy '/opt/twitter_mde") &&
      out.endsWith("Permission denied\n") &&
      out.linesIterator.size == 1
    ) {
      // ignore error, installation succeeded.
      0
    } else {
      app.cli.err.print(out)
      exit
    }
  }

}
