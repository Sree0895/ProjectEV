#!/usr/bin/env python
from time import sleep
import json
import paho.mqtt.client as mqtt

# This is the Publisher

subscribeTopic = "topic/Geeks/EV002324/userData/ssmr"
publishTopic = "topic/Geeks/EV002324/userService/ssmr/TS15EC1234"

dictionary ={
        "code" : "101",
		"user" : "ssmr",
		"evNumber" : "TS15EC1234",
		"evCharge" : "On"
	}

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    if rc==0:
        client.connected_flag=True
        client.subscribe(subscribeTopic)
    else:
        client.connected_flag=False

# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    #BackendParser(json.loads(msg.payload))
    print(msg.payload)    
        
stringOut = json.dumps(dictionary, indent = 4)
print(stringOut)
print(type(stringOut))
client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.connect("192.168.1.21",1883,60)
client.publish(publishTopic, stringOut);
client.loop_forever()
#client.disconnect()


