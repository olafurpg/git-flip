package gitmini

import java.nio.file.Path
import metaconfig.annotation.ExtraName
import java.nio.file.PathMatcher

final case class CountLinesOptions(
    exclude: List[String] = Nil,
    @ExtraName("remainingArgs")
    globs: List[String] = Nil
)
object CountLinesOptions {
  val default = CountLinesOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[CountLinesOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[CountLinesOptions](default)
}
