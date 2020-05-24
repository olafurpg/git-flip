package gitmini

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes

object StringFS {

  /**
    * Creates a temporary directory with a layout matching the markup in the string.
    *
   * Example syntax of the expected markup in the string:
    * {{{
    *   fromString("""
    *   /build.sbt
    *   lazy val core = project
    *   /src/main/scala/core/Foo.scala
    *   package core
    *   object Foo.scala
    *   """)
    * }}}
    *
   * Use `asString` for the inverse, go from a temporary directory to a string.
    *
   * @param layout the string representing the directory layout.
    *               NOTE. Lines starting with forward slash / are always interpreted
    *               as the start of a new file entry.
    * @param root the temporary directory to apply the layout markup.
    *             If not provided, defaults to a fresh temporary directory.
    */
  def fromString(
      layout: String,
      root: Path = Files.createTempDirectory("munit"),
      charset: Charset = StandardCharsets.UTF_8
  ): Path = {
    if (!layout.trim.isEmpty) {
      layout.split("(?=\n/)").foreach { row =>
        row.stripPrefix("\n").split("\n", 2).toList match {
          case path :: contents :: Nil =>
            val file = root.resolve(path.stripPrefix("/"))
            Files.createDirectories(file.getParent)
            Files.write(
              file,
              contents.getBytes(charset),
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING
            )
          case els =>
            throw new IllegalArgumentException(
              s"Unable to split argument info path/contents! \n$els"
            )

        }
      }
    }
    root
  }

  def asString(
      root: Path,
      charset: Charset = StandardCharsets.UTF_8
  ): String = {
    import scala.collection.JavaConverters._
    val buf = new StringBuilder()
    Files.walkFileTree(
      root,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            path: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          val relpath = root.relativize(path).iterator().asScala.mkString("/")
          val contents = new String(Files.readAllBytes(path), charset)
          buf
            .append("/")
            .append(relpath)
            .append("\n")
            .append(contents)
            .append("\n")
          FileVisitResult.CONTINUE
        }
      }
    )
    buf.toString()
  }

}
