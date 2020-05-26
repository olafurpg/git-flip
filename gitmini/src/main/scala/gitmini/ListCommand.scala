package gitmini

import metaconfig.cli.Command
import metaconfig.cli.CliApp
import java.nio.file.Files
import GitMiniEnrichments._
import org.typelevel.paiges.Doc
import scala.collection.mutable

object ListCommand extends Command[Unit]("list") {
  override def extraNames: List[String] = List("ls")
  override def description: Doc =
    Doc.text("List all installed git repos")
  def run(value: Value, cli: CliApp): Int = {
    val app = new Flip(cli)
    if (app.requireInstalled()) {
      1
    } else {
      all(app).foreach(r => cli.out.println(r))
      0
    }
  }
  def all(app: Flip): List[String] = {
    val ls = Files.list(app.gitmini)
    val buf = mutable.ListBuffer.empty[String]
    try {
      ls.forEach { p =>
        buf += p.getFileName().toString
      }
    } finally {
      ls.close()
    }
    buf.toList
  }
}
