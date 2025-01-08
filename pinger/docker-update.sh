#!/bin/bash

docker container stop pinger
docker container rm pinger

set -eu

docker build . -t pinger

docker run --network host --restart always -d --name pinger pinger

docker logs -f pinger


