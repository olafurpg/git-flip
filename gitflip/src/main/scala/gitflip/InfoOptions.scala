package gitflip

import metaconfig.annotation.ExtraName

final case class InfoOptions(
    @ExtraName("remainingArgs")
    minirepo: List[String] = Nil
)
object InfoOptions {
  val default = InfoOptions()
  implicit lazy val surface = metaconfig.generic.deriveSurface[InfoOptions]
  implicit lazy val codec = metaconfig.generic.deriveCodec[InfoOptions](default)
}
