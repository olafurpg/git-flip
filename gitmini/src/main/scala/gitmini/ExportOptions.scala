package gitmini

final case class ExportOptions()
object ExportOptions {
  val default = ExportOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[ExportOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[ExportOptions](default)
}
