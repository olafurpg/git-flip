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
import gitflip.internal.BuildInfo

object Gitflip {
  def app =
    CliApp(
      BuildInfo.version,
      "git-flip",
      commands = List(
        InstallCommand,
        UninstallCommand,
        StartCommand,
        StopCommand,
        SyncCommand,
        SwitchCommand,
        AmendCommand,
        ListCommand,
        CurrentCommand,
        InfoCommand,
        HelpCommand,
        VersionCommand,
        TabCompleteCommand
      )
    )

  def main(args: Array[String]): Unit = {
    val (argsList, cwd) = args.toList match {
      case "--cwd" :: path :: tail =>
        tail -> Paths.get(path).toAbsolutePath
      case tail =>
        tail -> Paths.get(".").toAbsolutePath().normalize()
    }
    val exit = app.copy(workingDirectory = cwd).run(argsList)
    if (exit != 0) System.exit(exit)
  }
}
