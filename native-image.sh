set -eux
NATIVE_IMAGE=$(~/.jabba/bin/jabba which --home graalvm@20.0.0)/bin/native-image
$NATIVE_IMAGE -cp $(cs fetch --classpath com.geirsson:git-flip_2.13:0.1.0-SNAPSHOT) --initialize-at-build-time --initialize-at-run-time=metaconfig gitflip.Gitflip ~/bin/git-flip
say 'native-image ready'
