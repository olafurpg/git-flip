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
import gitflip.GitFlipEnrichments._
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object Gitflip {

  def main(args: Array[String]): Unit = {
    val app = CliApp(
      "0.1.0",
      "git-flip",
      commands = List(
        InitCommand,
        CreateCommand,
        AddCommand,
        SwitchCommand,
        PushCommand,
        PullCommand,
        AmendCommand,
        ListCommand,
        InfoCommand,
        HelpCommand,
        VersionCommand,
        TabCompleteCommand
      )
    )
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
