package gitflip

import metaconfig.cli.CliApp
import metaconfig.cli.Command

object PullCommand extends Command[Unit]("pull") {
  def run(value: Value, app: CliApp): Int = {
    0
  }
}
