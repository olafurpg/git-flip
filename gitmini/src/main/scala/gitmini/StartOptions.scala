package gitmini

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class StartOptions(
    name: String = "",
    @ExtraName("remainingArgs")
    directories: List[Path] = Nil
)

object StartOptions {
  val default = StartOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[StartOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[StartOptions](default)
}
