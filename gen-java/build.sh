#!/usr/bin/env bash

SRC_API_QEUEUE="../api"

APIS_OUT="src/main/java/"

APIS_OUT_QUEUE="queue/$APIS_OUT"


protoc -I../ $SRC_API_QEUEUE/*.proto --java_out=$APIS_OUT_QUEUE --grpc-java_out=$APIS_OUT_QUEUE --plugin=protoc-gen-grpc-java=$GOPATH/bin/protoc-gen-grpc-java

