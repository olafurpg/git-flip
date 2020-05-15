package gitflip

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class CreateOptions(
    name: Option[String] = None,
    @ExtraName("remainingArgs")
    directories: List[Path] = Nil
)
object CreateOptions {
  val default = CreateOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[CreateOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[CreateOptions](default)
}
