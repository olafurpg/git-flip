package gitflip

import metaconfig.cli.CliApp
import java.nio.file.Path
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.file.Files
import java.nio.charset.StandardCharsets

object GitFlipEnrichments {
  implicit class XtensionPath(path: Path) {
    def readText: String =
      new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }
  implicit class XtensionCliApp(app: CliApp) {
    def git: Path = app.workingDirectory.resolve(".git")
    def gitignore: Path = app.workingDirectory.resolve(".gitignore")
    def gitflipName: String = ".git-flip"
    def gitflip: Path = app.workingDirectory.resolve(gitflipName)
    def megarepo: Path = gitflip.resolve("megarepo")
    def readLine(message: String): String = {
      app.info(message)
      new BufferedReader(new InputStreamReader(app.in)).readLine()
    }
    def minirepo(name: String): Path = {
      gitflip.resolve(name)
    }
    def exclude(name: String): Path = {
      minirepo(name).resolve("info").resolve("exclude")
    }
    def includes(name: String): Path = {
      minirepo(name).resolve("info").resolve("includes")
    }
    def exec(command: List[String]): Int = {
      scala.sys.process
        .Process(command, cwd = Some(app.workingDirectory.toFile()))
        .!
    }
    def exec(command: String*): Int = {
      exec(command.toList)
    }
  }

}
