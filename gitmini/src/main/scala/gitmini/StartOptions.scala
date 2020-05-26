package gitmini

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class StartOptions(
    name: String = "",
    @ExtraName("remainingArgs")
    directories: List[Path] = Nil
) {
  def isMissingExplicitName: Boolean =
    name.isEmpty() && {
      directories match {
        case Nil         => true
        case head :: Nil => head.getNameCount() > 1
        case _           => true
      }
    }
  def minirepoName: String =
    if (!name.isEmpty()) {
      name
    } else {
      directories match {
        case Nil       => ""
        case head :: _ => head.getFileName().toString()
      }
    }
}

object StartOptions {
  val default = StartOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[StartOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[StartOptions](default)
}
