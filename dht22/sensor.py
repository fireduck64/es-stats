#!/usr/bin/python3

import time
import sys
import json
import adafruit_dht

# apt-get install python3-pip libgpiod-dev
# pip3 install adafruit-circuitpython-dht

pin = int(sys.argv[1])

dht_device = adafruit_dht.DHT22(pin)

temperature = dht_device.temperature
humidity = dht_device.humidity

report = { 
  "humidity": round(humidity,3),
  "temperature": round(temperature,3)
}
if (humidity > 105.0) or (temperature > 100.0) or (temperature < -70.0) or (humidity < 0.0):
  report = { "error": "nonsense values" }

print(json.dumps(report));


