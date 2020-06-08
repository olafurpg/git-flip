package gitmini

import metaconfig.cli.Command
import java.nio.file.Path
import metaconfig.cli.CliApp
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import scala.sys.process.ProcessLogger
import java.nio.file.PathMatcher
import java.nio.file.Paths
import scala.util.control.NonFatal
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import java.nio.charset.StandardCharsets
import scala.tools.nsc.javac.JavaScanners
import scala.tools.nsc.ast.parser.CommonTokens
import scala.tools.nsc.interactive.NoSuchUnitError
import scala.tools.nsc.reporters.StoreReporter

object CountLinesCommand extends Command[CountLinesOptions]("count") {
  val javaOrScalaPattern =
    FileSystems.getDefault().getPathMatcher("glob:**.{java,scala}")
  val javaPattern = FileSystems.getDefault().getPathMatcher("glob:**.java")
  val scalaPattern = FileSystems.getDefault().getPathMatcher("glob:**.scala")
  val javaLines = new AtomicInteger()
  val scalaLines = new AtomicInteger()
  def parseGlob(glob: String): PathMatcher =
    FileSystems.getDefault().getPathMatcher(s"glob:$glob")
  def run(value: Value, app: CliApp): Int = {
    val includeGlobs = value.globs match {
      case Nil   => List("**.{java,scala}")
      case globs => globs
    }
    val includes = includeGlobs.map(parseGlob)
    val excludes = value.exclude.map(parseGlob)
    val lines = mutable.ListBuffer.empty[String]
    lazy val global = {
      val s = new Settings()
      s.usejavacp.value = true
      s.classpath.value =
        "/Users/lgeirsson/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.2/scala-library-2.13.2.jar"
      val r = new StoreReporter(s)
      new scala.tools.nsc.interactive.Global(s, r)
    }
    val exit = scala.sys.process
      .Process(
        List("git", "ls-files"),
        cwd = Some(app.workingDirectory.toFile())
      )
      .!(ProcessLogger(out => lines += out, err => app.error(err)))
    if (exit != 0) exit
    else {
      try walkLines(lines, includes, excludes, global, app)
      finally global.askShutdown()
      app.info(s"scala ${scalaLines.get()}")
      app.info(s"java  ${javaLines.get()}")
      0
    }
  }

  def walkLines(
      lines: Iterable[String],
      includes: List[PathMatcher],
      excludes: List[PathMatcher],
      global: Global,
      app: CliApp
  ): Unit = {
    lines.foreach { path =>
      try {
        val relativePath = Paths.get(path)
        val isJavaOrScala = javaOrScalaPattern.matches(relativePath)
        val isIncluded = includes.exists(_.matches(relativePath))
        val isExcluded = excludes.exists(_.matches(relativePath))
        if (isJavaOrScala && isIncluded && !isExcluded) {
          val absolutePath = app.workingDirectory.resolve(relativePath)
          countLines(absolutePath, global)
        }
      } catch {
        case NonFatal(e) =>
          e.printStackTrace()
      }
    }

  }

  def countLines(path: Path, global: Global): Unit = {
    val text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    val filename = path.toString()
    val unit = global.newCompilationUnit(text, filename)
    if (javaPattern.matches(path)) {
      javaLines.addAndGet(countJavaLines(global)(unit))
    } else {
      scalaLines.addAndGet(countScalaLines(global)(unit))
    }
  }

  val t = new CommonTokens {
    def isIdentifier(code: Int): Boolean = false
    def isLiteral(code: Int): Boolean = false
  }

  class Lines() {
    var count = 0
    private var oldLine = -1
    def hitLine(currentLine: Int): Unit = {
      if (currentLine > oldLine) {
        count += 1
        oldLine = currentLine
      }
    }
  }
  def countJavaLines(global: Global)(unit: global.CompilationUnit): Int = {
    val in = new global.syntaxAnalyzer.JavaUnitScanner(unit)
    val lines = new Lines()
    while (true) {
      in.token match {
        case t.EOF | t.ERROR | t.UNDEF => return lines.count
        case t.RBRACE                  =>
        case token =>
          lines.hitLine(in.currentPos.line)
      }
      in.nextToken()
    }
    lines.count
  }

  def countScalaLines(global: Global)(unit: global.CompilationUnit): Int = {
    val in = new global.syntaxAnalyzer.UnitScanner(unit)
    in.nextChar()
    val lines = new Lines()
    while (true) {
      in.token match {
        case t.EOF | t.ERROR | t.UNDEF => return lines.count
        case t.RBRACE                  =>
        case token =>
          val line = unit.source.offsetToLine(in.offset)
          lines.hitLine(line)
      }
      in.nextToken()
    }
    lines.count
  }
}
