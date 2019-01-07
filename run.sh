#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
gradle -p $SCRIPTPATH run --args="$(realpath $1)"
