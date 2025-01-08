#!/bin/bash

docker container stop pinger
docker container rm pinger

set -eu

docker build . -t pinger

export PINGER_SOURCE=ogog
export PINGER_TARGETS=hippo,potato,soup,noface,orange,flow,web-relay


docker run --network host --restart always -d --name pinger \
  -e PINGER_SOURCE=ogog \
  -e PINGER_TARGETS=hippo,potato,soup,noface,orange,flow,web-relay \
  pinger

docker logs -f pinger


