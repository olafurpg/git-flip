package gitmini

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class CreateOptions(
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

object CreateOptions {
  val default = CreateOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[CreateOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[CreateOptions](default)
}
