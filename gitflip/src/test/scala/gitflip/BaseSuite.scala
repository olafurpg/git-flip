package gitflip

import java.nio.file.Path
import java.nio.file.Files
import metaconfig.cli.CliApp
import gitflip.internal.BuildInfo
import java.nio.charset.StandardCharsets
import GitflipEnrichments._
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import munit.GenericAfterEach
import scala.concurrent.Future
import java.io.ByteArrayInputStream
import scala.math.Numeric.ByteIsIntegral
import java.nio.file.Paths
import scala.util.Properties
import java.io.InputStream
import scala.io.StdIn
import java.nio.file.StandardOpenOption
import scala.util.Failure

abstract class BaseSuite extends munit.FunSuite {
  def freeze(): Unit = {
    println(s"cd ${repo()}")
    StdIn.readLine("press enter to continue: ")
  }
  val repo = new Fixture[Path]("repo") {
    var p: Path = _
    var filename = ""
    def apply(): Path = p.resolve(filename)
    override def beforeAll(): Unit = {
      p = Files.createTempDirectory("git-flip")
      if (Properties.isMac && p.startsWith("/var")) {
        // NOTE(olafur): I'm not entirely sure why this is needed, but
        // Files.isSameFile(Paths.get("/var"), Paths.get("/private/var"))
        // returns true. The /var directory is some kind of symbolic link to
        // /private/var on macOS even if `Files.isDirectory(Paths.get("/var"))`
        // returns true. The command `git rev-parse --show-toplevel` returns the
        // `/private/var` path and `Files.createTempDirectory()` returns the
        // `/var` path, causing issues in how git-flip relativizes include
        // paths. This line ensures that git-flip always uses the same path as
        // the --show-toplevel directory returned by git.
        p = Paths.get(s"/private/$p")
      }
    }
    override def beforeEach(context: BeforeEach): Unit = {
      filename = context.test.name
    }
    override def afterAll(): Unit = {
      val exit = DeleteVisitor.deleteRecursively(p, new Flip(Gitflip.app))

      assertEquals(exit, 0)
    }
  }
  val stdout = new Fixture[ByteArrayOutputStream]("stdout") {
    private val baos = new ByteArrayOutputStream()
    private val out = new PrintStream(baos)
    def apply(): ByteArrayOutputStream = baos
    override def beforeEach(context: BeforeEach): Unit = {
      baos.reset()
    }
  }
  val stdin = new InputStream {
    var isYes = false
    def read(): Int = {
      isYes = !isYes
      if (isYes) 'y'.toInt
      else '\n'.toInt
    }
  }
  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms ++ List(
      new TestTransform(
        "stdout",
        _.withBodyMap {
          _.transform(
            identity,
            e => {
              println(stdout())
              e
            }
          )(munitExecutionContext)
        }
      )
    )

  override def munitFixtures: Seq[Fixture[_]] =
    super.munitFixtures ++ List(
      repo,
      stdout
    )
  def app =
    new Flip(
      Gitflip.app.copy(
        workingDirectory = repo(),
        out = new PrintStream(stdout()),
        err = new PrintStream(stdout()),
        in = stdin
      )
    )
  def exec(command: String*): Unit = {
    val exit = app.exec(command.toList)
    assertEquals(exit, exit, clues(command, stdout()))
  }
  def execString(command: String*): String = {
    val logger = new CaptureLogger
    app.exec(command.toList, logger = logger)
    logger.baos.toString()
  }

  def init(layout: String): Unit = {
    StringFS.fromString(layout, repo())
    exec("git", "init")
    exec("git", "add", ".")
    exec("git", "commit", "-m", "First commit")
    exec("git", "branch", "-f", "origin/master", "master")
  }
  def lsFiles(): List[String] =
    execString("git", "ls-files").linesIterator.toList.sorted
  def touch(filename: String, text: String = ""): Path = {
    val path = repo().resolve(filename)
    Files.createDirectories(path.getParent())
    Files.write(path, text.getBytes(), StandardOpenOption.CREATE_NEW)
    path
  }
  def run(command: String*)(implicit loc: munit.Location): Unit = {
    val exit = app.cli.run(command.toList)
    assertEquals(exit, 0, clues(command, stdout()))
  }

}
