import time

import urequests

from dht import DHT22
from machine import Pin

sensor = DHT22(Pin(4))

def callback(t):
  sensor.measure()
  urequests.post("http://10.3.1.10:9200/airsense-today/_doc", 
    json=
      {
        "temperature": sensor.temperature(), 
        "humidity": sensor.humidity(),
        "zone": "crabshack",
        "location": "chest_freezer"
      })

from machine import Timer
timer = Timer(-1)
timer.init(mode=Timer.PERIODIC, period=60000, callback=callback)


