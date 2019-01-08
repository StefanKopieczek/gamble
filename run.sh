#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
gradle -p $SCRIPTPATH run -PappArgs="['$(realpath $1)']"
