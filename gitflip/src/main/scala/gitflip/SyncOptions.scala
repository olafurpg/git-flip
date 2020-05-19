package gitflip

final case class SyncOptions(
    rebase: Boolean = false
)
object SyncOptions {
  val default = SyncOptions()
  implicit lazy val surface =
    metaconfig.generic.deriveSurface[SyncOptions]
  implicit lazy val codec =
    metaconfig.generic.deriveCodec[SyncOptions](default)
}
