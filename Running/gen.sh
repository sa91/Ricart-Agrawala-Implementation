
set -e -x
#protoc --java_out=. -I. clock.proto
javac -d . -cp protobuf-java-3.0.2.jar:. ../src/DistributedLocking/*.java
