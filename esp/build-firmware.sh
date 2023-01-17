#!/bin/bash

set -eu

docker pull larsks/esp-open-sdk

if [ ! -e micropython.git ]
then
  git clone https://github.com/micropython/micropython.git micropython.git
fi

cd micropython.git
docker run --rm -v $HOME:$HOME -u $UID -w $PWD larsks/esp-open-sdk make -C mpy-cross
docker run --rm -v $HOME:$HOME -u $UID -w $PWD larsks/esp-open-sdk make -C ports/esp8266 submodules

cd ports/esp8266
docker run --rm -v $HOME:$HOME -u $UID -w $PWD larsks/esp-open-sdk make -j BOARD=GENERIC
docker run --rm -v $HOME:$HOME -u 0 -w $PWD --device=/dev/ttyUSB0:/dev/ttyACM0 larsks/esp-open-sdk make -j BOARD=GENERIC deploy

