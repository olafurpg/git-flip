package gitflip

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import gitflip.GitFlipEnrichments._
import scala.jdk.CollectionConverters._
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import org.typelevel.paiges.Doc

object CreateCommand extends Command[CreateOptions]("create") {
  override def description: Doc = Doc.text("Create new git minirepo")
  def run(value: Value, app: CliApp): Int = {
    if (value.directories.isEmpty) {
      app.error(
        s"can't create a new minirepo from an empty list of directories to exclude. " +
          s"To fix this problem, pass the directory that you wish to exclude. For example:\n\t" +
          s"${app.binaryName} create my-directory"
      )
      1
    } else {
      value.name match {
        case None =>
          app.error(
            "missing --name. To fix this problem, provide a name for the new minirepo:\n\t" +
              s"${app.binaryName} create --name MINIREPO_NAME ${value.directories.mkString(" ")}"
          )
          1
        case Some(name) =>
          val minirepo = app.minirepo(name)
          if (Files.isDirectory(minirepo)) {
            app.error(
              s"can't create minirepo '$name' because it already exists.\n\t" +
                s"To amend this minirepo run: ${app.binaryName} amend $name"
            )
            1
          } else {
            if (InitCommand.run((), app) == 0) {
              require(
                Files.isRegularFile(app.git),
                "git-flip is not initialized!"
              )
              val backup = Files.createTempFile("git-flip", ".git")
              Files.move(app.git, backup, StandardCopyOption.REPLACE_EXISTING)
              if (
                app.exec(
                  "git",
                  "init",
                  "--separate-git-dir",
                  minirepo.toString()
                ) == 0
              ) {
                val exclude = app.exclude(name)
                val includes = app.includes(name)
                val includeDirectories = value.directories.map(_.toString())
                // TODO: validate the number of files to add is smaller than 50k
                Files.write(
                  exclude,
                  List("*").asJava,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.TRUNCATE_EXISTING
                )
                Files.write(
                  includes,
                  includeDirectories.asJava,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.TRUNCATE_EXISTING
                )
                if (
                  app.exec(
                    List("git", "add", "--force") ++ includeDirectories
                  ) == 0
                ) {
                  app.exec(
                    "git",
                    "commit",
                    "-m",
                    s"First commit in minirepo $name"
                  )
                } else {
                  1
                }
              } else {
                Files.move(backup, app.git)
                app.error(s"Failed to create new minirepo named '$name'")
                1
              }
            } else {
              1
            }
          }
      }
    }
  }
}
