#!/usr/bin/python3

import Adafruit_DHT
import RPi.GPIO as GPIO
import time

# apt-get install python3-pip
# pip3 install RPi.GPIO Adafruit_DHT

DHT_PIN = 4
GPIO.setmode(GPIO.BCM)

#GPIO.setup(DHT_PIN, GPIO.IN, pull_up_down=GPIO.PUD_UP)

DHT_SENSOR = Adafruit_DHT.DHT22


while True:
  humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)
  if humidity is not None and temperature is not None:
  	print("Temp={0:0.1f}*C  Humidity={1:0.1f}%".format(temperature, humidity))
  else:
    print("Failed to retrieve data from humidity sensor")
  time.sleep(2);


