package gitflip

import metaconfig.cli.CliApp
import java.nio.file.Path
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import fansi.Color
import java.nio.file.Paths
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object GitflipEnrichments {

  implicit class XtensionExitCode(exit: Int) {
    def map(fn: Int => Int): Int =
      if (exit == 0) fn(exit)
      else exit
    def flatMap(fn: Int => Int): Int =
      if (exit == 0) fn(exit)
      else exit
    def ifSuccessful(thunk: => Int): Int = {
      if (exit == 0) thunk
      else exit
    }
  }

  implicit class XtensionPath(path: Path) {
    def listForeach(fn: Path => Unit): Unit = {
      val ls = Files.list(path)
      try ls.forEach(p => fn(p))
      finally ls.close()
    }
    def readText: String =
      new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
  }

}
