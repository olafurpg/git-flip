package gitmini

import scala.sys.process.ProcessLogger
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CaptureLogger extends ProcessLogger {
  val baos = new ByteArrayOutputStream
  val ps = new PrintStream(baos)
  def out(s: => String): Unit = ps.println(s)
  def err(s: => String): Unit = ps.println(s)
  def buffer[T](f: => T): T = f
  override def toString(): String = baos.toString()
}
