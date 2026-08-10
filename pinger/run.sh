#!/bin/bash

set -eu

bazel build :all

time bazel-bin/Pinger -Dsun.net.inetaddr.negative.ttl=0 $*


