package gitmini

import metaconfig.cli.CliApp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.sys.process.ProcessLogger
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import fansi.Color
import GitMiniEnrichments._
import java.io.PrintWriter
import metaconfig.internal.Levenshtein

class Flip(val cli: CliApp) {
  lazy val toplevel: Path =
    Paths
      .get(
        execString(List("git", "rev-parse", "--show-toplevel")).trim
      )
      .normalize()
  def minirepoBranch(): String =
    execString(List("git", "rev-parse", "--abbrev-ref", "HEAD")).trim
  def megarepoBranch(): String =
    execString(
      List(
        "git",
        s"--git-dir=$megarepo",
        "rev-parse",
        "--abbrev-ref",
        "HEAD"
      )
    ).trim
  def megarepoCommit(): String =
    execString(
      List(
        "git",
        s"--git-dir=$megarepo",
        "rev-parse",
        "origin/master"
      )
    ).trim
  def syncToMegarepoMessage(): String =
    s"$binaryName: sync to $megarepoName ${megarepoCommit()}"
  lazy val flipBranchName: String = {
    s"${System.getProperty("user.name")}/git-mini/$flipRepoName/$minirepoBranch"
  }
  lazy val flipRepoName: String = {
    git.readText.linesIterator.toList
      .collectFirst {
        case s"gitdir: $path" => Paths.get(path).getFileName().toString()
      }
      .getOrElse {
        sys.error(s"$binaryName is not installed: $git")
      }
  }
  def checkoutMegarepoBranch(): Int = {
    val targetName = flipBranchName
    val actualName = megarepoBranch
    if (actualName == targetName) 0
    else {
      val logger = new CaptureLogger
      val checkout = exec(
        List("git", s"--git-dir=$megarepo", "checkout", targetName),
        logger = logger
      )
      if (checkout == 0) 0
      else {
        exec("git", s"--git-dir=$megarepo", "checkout", "-b", targetName)
      }
    }
  }
  def toAbsolutePath(path: Path): Path = {
    if (path.isAbsolute()) path
    else cli.workingDirectory.resolve(path)
  }
  def isGitRepository: Boolean =
    try {
      toplevel
      true
    } catch {
      case NonFatal(_) =>
        false
    }
  def git: Path = toplevel.resolve(".git")
  def gitignore: Path = toplevel.resolve(".gitignore")
  def gitmini: Path =
    toplevel
      .resolve("..")
      .resolve(s"git-minis")
      .resolve(toplevel.getFileName())
      .normalize()
  def megarepo: Path =
    gitmini.resolve(megarepoName)
  def megarepoName = "megarepo"
  def minirepo(name: String): Path = {
    gitmini.resolve(name)
  }
  def exclude(name: String): Path = {
    minirepo(name).resolve("info").resolve("exclude")
  }
  def includes(name: String): Path = {
    minirepo(name).resolve("info").resolve("includes")
  }
  def readLine(message: String): String = {
    cli.err.print(message)
    new BufferedReader(new InputStreamReader(cli.in)).readLine()
  }
  def readIncludes(name: String, printWarnings: Boolean): Set[Path] = {
    import scala.jdk.CollectionConverters._
    val i = includes(name)
    if (Files.isRegularFile(i)) {
      val t = toplevel
      val paths = Set.newBuilder[Path]
      val lines = Files.lines(i)
      try lines.forEach { line =>
        val dir = Paths.get(line)
        val relativePath = dir.getName(0)
        if (printWarnings && dir.getNameCount() != 1) {
          cli.warn(
            s"converting argument '$dir' into '$relativePath'. " +
              s"Due to an implementation limitation, ${cli.binaryName} does not " +
              s"support tracking nested subdirectories, only toplevel subdirectories."
          )
        }
        paths += toplevel.resolve(relativePath)
      } finally {
        lines.close()
      }
      paths.result()
    } else {
      Set.empty
    }
  }
  private def defaultLogger = ProcessLogger(out => cli.err.println(out))
  def exec(
      command: List[String],
      logger: ProcessLogger = defaultLogger
  ): Int = {
    cli.info(command.mkString(" "))
    scala.sys.process
      .Process(command, cwd = Some(cli.workingDirectory.toFile()))
      .!(logger)
  }
  def exec(command: String*): Int = {
    exec(command.toList)
  }
  def execString(
      command: List[String],
      isSilent: Boolean = true
  ): String = {
    if (!isSilent) {
      val commands =
        command.map(c => if (c.contains(' ')) s""""$c"""" else c).mkString(" ")
      info(commands)
    }
    val logger = new CaptureLogger()
    val exit = scala.sys.process
      .Process(command, cwd = Some(cli.workingDirectory.toFile()))
      .!(logger)
    if (exit == 0) logger.baos.toString()
    else {
      throw new RuntimeException(
        s"non-zero exit code $exit running command $command:\n$logger"
      )
    }
  }
  def confirm(message: String): Either[Int, Boolean] = {
    val result = Try(
      readLine((Color.LightYellow("warn: ") ++ message).toString)
    )
    result match {
      case Success("y" | "Y" | "Yes" | "yes") =>
        Right(true)
      case Success("n" | "N" | "No" | "no" | null) =>
        Right(false)
      case Success(other) =>
        cli.error(
          s"unknown response '$other'. Expected 'y' for test or 'n' for no."
        )
        Left(1)
      case Failure(exception) =>
        exception.printStackTrace(cli.err)
        Left(1)
    }
  }

  def currentName(): Option[String] = {
    if (Try(toplevel).isFailure) {
      cli.error(s"${cli.binaryName} must run inside a git repository")
      None
    } else if (!Files.isRegularFile(git)) {
      cli.error(s"${cli.binaryName} is not installed")
      None
    } else {
      git.readText.linesIterator.toList.collectFirst {
        case s"gitdir: $path" => Paths.get(path).getFileName().toString()
      }
    }
  }

  def verifyInstallation(): Int = {
    if (Try(toplevel).isFailure) {
      cli.error(s"${cli.binaryName} can only run inside a git repository")
      1
    } else {
      0
    }
  }

  def execTty(command: String): Int = {
    try {
      // Adjusted from https://stackoverflow.com/questions/29733038/running-interactive-shell-program-in-java
      val proc = Runtime
        .getRuntime()
        .exec(Array("/bin/bash"), null, cli.workingDirectory.toFile())
      val stdin = proc.getOutputStream()
      val pw = new PrintWriter(stdin)
      pw.println(s"$command < /dev/tty > /dev/tty")
      pw.close()
      proc.waitFor()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace(cli.out)
        1
    }
  }

  def withMinirepo(what: String, minirepos: List[String])(
      operation: String => Int
  ): Int = {
    minirepos.headOption match {
      case None =>
        cli.error(
          s"can't $what to a minirepo since no argument was provided.\n\t" +
            s"To list existing minirepos run: ${cli.binaryName} list"
        )
        1
      case Some(name) =>
        val minirepo = this.minirepo(name)
        if (!Files.isRegularFile(git)) {
          cli.error(
            s"not a regular file '${git}'. " +
              s"Did you run '${cli.binaryName} install'?"
          )
          1
        } else if (!Files.isDirectory(minirepo)) {
          noSuchminirepo(name)
        } else {
          operation(name)
        }
    }

  }

  def noSuchminirepo(name: String): Int = {
    val candidates = ListCommand.all(this)
    val closest = Levenshtein.closestCandidate(name, candidates)
    val didYouMean = closest match {
      case Some(candidate) => s"\n\tDid you mean '$candidate'?"
      case None            => ""
    }
    cli.error(s"minirepo '$name' does not exist$didYouMean")
    1
  }
  def binaryName = cli.binaryName
  def error(message: String) = cli.error(message)
  def info(message: String) = cli.info(message)
  def status(): List[String] =
    execString(
      List("git", "status", "--porcelain"),
      isSilent = false
    ).linesIterator.toList
  def requireInstalled(): Boolean = {
    if (Files.isRegularFile(git) && Files.isDirectory(gitmini)) {
      false
    } else {
      error(s"$binaryName is not installed")
      true
    }
  }
  def requireBranch(branchName: String, what: String): Boolean = {
    val isWrongBranch = minirepoBranch() != branchName
    if (isWrongBranch)
      error(
        s"can only run ${cli.binaryName} $what when on the branch '$branchName'. " +
          s"To fix this problem run:" +
          s"\n\tgit checkout master"
      )
    isWrongBranch
  }
  def requireMasterBranchIsUpToDate(what: String): Boolean = {
    val master = execString(List("git", "rev-parse", "master"))
    val origin = execString(List("git", "rev-parse", "origin/master"))
    val isDifferent = master != origin
    if (isDifferent) {
      error(
        s"can only run ${cli.binaryName} $what when the master branch is up to date with origin/master. " +
          s"To fix this problem, run:" +
          s"\n\tgit merge origin/master"
      )
    }
    isDifferent
  }
  def requireInsideMegarepo(what: String): Boolean = {
    currentName() match {
      case Some(name) =>
        val isOutside = name != megarepoName
        if (isOutside)
          error(
              s"To fix this problem run:" +
              s"\n\t${cli.binaryName} switch $megarepoName"
          )
        isOutside
      case None =>
        false
    }
  }
  def requireCleanStatus(): Boolean = {
    val isDirty = status().nonEmpty
    if (isDirty)
      error(
        "can't create a minirepo with uncommitted changes. " +
          "To fix this problem, commit your unsaved changes first:" +
          "\n\tgit add . && git commit"
      )

    isDirty
  }
  def checkoutOriginMaster(): Int = {
    exec("git", "checkout", "origin/master")
  }
  def pullOriginMasterInMegarepo(): Int = {
    exec(
      "git",
      s"--git-dir=${megarepo}",
      "pull",
      "origin",
      "master"
    )
  }
  def commitAll(message: String): Int = {
    exec(List("git", "add", ".")).ifSuccessful {
      exec(
        "git",
        "commit",
        "--allow-empty",
        "-m",
        message
      )
    }
  }
}