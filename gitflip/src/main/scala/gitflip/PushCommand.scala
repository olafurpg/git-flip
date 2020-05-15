package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command

object PushCommand extends Command[Unit]("push") {
  def run(value: Value, app: CliApp): Int = {
    0
  }
}
