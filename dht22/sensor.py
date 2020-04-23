#!/usr/bin/python3

import Adafruit_DHT
import RPi.GPIO as GPIO
import time

# apt-get install python3-pip
# pip3 install RPi.GPIO Adafruit_DHT

DHT_PIN = int(sys.argv[1])
GPIO.setmode(GPIO.BCM)

#GPIO.setup(DHT_PIN, GPIO.IN, pull_up_down=GPIO.PUD_UP)

DHT_SENSOR = Adafruit_DHT.DHT22

humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)
if humidity is not None and temperature is not None:
  print('{"temp":{0:0.1f},"humidity":{1:0.1f}%}'.format(temperature, humidity))
else:
  print('{"error":"Failed to retrieve data from humidity sensor")')


