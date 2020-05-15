package gitflip

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class AddOptions(
    @ExtraName("remainingArgs")
    minirepo: List[String] = Nil
)
object AddOptions {
  val default = AddOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[AddOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[AddOptions](default)
}
