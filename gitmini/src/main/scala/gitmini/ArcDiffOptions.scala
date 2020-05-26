package gitmini

import metaconfig.annotation.CatchInvalidFlags
import metaconfig.annotation.ExtraName
import metaconfig.internal.CliParser

final case class ArcDiffOptions(
    @CatchInvalidFlags()
    @ExtraName(CliParser.PositionalArgument)
    arguments: List[String] = Nil
)
object ArcDiffOptions {
  val default = ArcDiffOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[ArcDiffOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[ArcDiffOptions](default)
}
