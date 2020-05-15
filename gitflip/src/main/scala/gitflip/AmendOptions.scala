package gitflip

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class AmendOptions(
    @ExtraName("remainingArgs")
    minirepo: List[String] = Nil
)
object AmendOptions {
  val default = AmendOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[AmendOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[AmendOptions](default)
}
