#!/usr/bin/python3

import Adafruit_DHT
import RPi.GPIO as GPIO
import time
import sys
import json

# apt-get install python3-pip
# pip3 install RPi.GPIO Adafruit_DHT

DHT_PIN = int(sys.argv[1])
GPIO.setmode(GPIO.BCM)

#GPIO.setup(DHT_PIN, GPIO.IN, pull_up_down=GPIO.PUD_UP)

DHT_SENSOR = Adafruit_DHT.DHT22

humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)
report = { 
  "humidity": round(humidity,3),
  "temperature": round(temperature,3)
}
if (humidity > 105.0) or (temperature > 100):
  report = { "error": "nonsense values" }


print(json.dumps(report));


