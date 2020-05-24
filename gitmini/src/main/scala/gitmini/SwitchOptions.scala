package gitmini

import java.nio.file.Path
import metaconfig.annotation.ExtraName

final case class SwitchOptions(
    @ExtraName("remainingArgs")
    minirepo: List[String] = Nil
)
object SwitchOptions {
  val default = SwitchOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[SwitchOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[SwitchOptions](default)
}
