#!/bin/bash

docker container stop radgrab
docker container rm radgrab

set -eu

docker build . -t radgrab

docker run --network host --restart always -d --name radgrab radgrab

docker logs -f radgrab


