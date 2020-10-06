#!/usr/bin/env bash

SRC_API_DIR="../api"
INCLUDES_APIS="-I../ "

GO_OUT="plugins=grpc,paths=source_relative:."

PROTOC="protoc"

echo $PROTOC $INCLUDES_APIS $SRC_API_DIR/*.proto --go_out=$GO_OUT
$PROTOC $INCLUDES_APIS $SRC_API_DIR/*.proto --go_out=$GO_OUT
