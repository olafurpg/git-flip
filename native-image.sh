set -eux
~/.jabba/bin/jabba use graalvm@20.0.0
native-image -cp $(cs fetch --classpath com.geirsson:git-flip_2.13:0.1.0-SNAPSHOT) --initialize-at-build-time --initialize-at-run-time=metaconfig gitflip.Gitflip ~/bin/git-flip
say 'native-image ready'
