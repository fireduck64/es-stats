import network
sta_if = network.WLAN(network.STA_IF)
sta_if.active(True)
sta_if.connect("TRCUK", "aaaaaaaaaaaaaaaaaaaa")

import time

import urequests

from dht import DHT22
from machine import Pin

sensor = DHT22(Pin(4))

def callback(t):
  sensor.measure()
  urequests.post("http://10.134.32.1:9200/airsense-today/_doc", 
    json=
      {
        "temperature": sensor.temperature(), 
        "humidity": sensor.humidity(),
        "zone": "garage",
        "location": "freezer"
      })

from machine import Timer
timer = Timer(-1)
timer.init(mode=Timer.PERIODIC, period=300000, callback=callback)


