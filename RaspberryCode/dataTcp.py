#!/usr/bin/env python3

import socket
import json
import time
import queue

from time import sleep
from PyQt5.QtGui import QGuiApplication
from PyQt5.QtCore import QUrl, QTimer, pyqtSignal, QObject
import sys
import os
import threading
import random

TCP_IP_HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
TCP_IP_PORT = 9999         # Port to listen on (non-privileged ports are > 1023)

class Backend(QObject):
    chargeValue = 0.1
    batDect=0
    currVolt=2.0
    currAmpere=0.1
    def __init__(self):
        super().__init__()
        
        self.scheduler = taskScheduler()
        
        # Define timer.
        self.timer = QTimer()
        self.timer.setInterval(3000)  # msecs 100 = 1/10th sec
        self.timer.timeout.connect(self.update_time)
        self.timer.start()

        self.timer2 = QTimer()
        self.timer2.setInterval(20)  # msecs 100 = 1/10th sec
        self.timer2.timeout.connect(self.executeTask)
        self.timer2.start()
        
    def update_time(self):
        if(Backend.batDect == 1):
            Backend.chargeValue= Backend.chargeValue+ 0.01
            if(Backend.chargeValue > 1.0):
                Backend.chargeValue = 1.0
            
    def executeTask(self):    
         # Execute the task
        self.scheduler.executeFromTaskQueue()            

class taskScheduler():
    global qTaskList

    qTaskList = queue.Queue()     
    def __init__(self):
        self.tcpCon = tcpServerClient("server")
        print("Initiated Task Scheduler class")

    @staticmethod
    def addToTaskQueue(item):
        if not qTaskList.full():
            #print("Task added")
            qTaskList.put(item)

    def executeFromTaskQueue(self):
        if not qTaskList.empty():
            item = qTaskList.get()
            if("mqtt" == item["commType"]):
                if("tx" == item["transactionType"]):
                    pubTopic = item["topic"]
                    del item["commType"]
                    del item["transactionType"]
                    del item["topic"]
                    #self.mqttCon.mqttSend(item,pubTopic)
                elif("rx" == item["transactionType"]):
                    pass
            elif("tcp" == item["commType"]):
                if("tx" == item["transactionType"]):
                    del item["commType"]
                    del item["transactionType"]
                    tcpServerClient.sendTcpData(item)
                elif("rx" == item["transactionType"]):
                    BackendParser(item)

def BackendParser(msg):       
    if(msg["code"] == "5001"):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5001"     
        TaskDict["evCharge"]= "On"
        Backend.chargeValue = random.uniform(0, 1)
        TaskDict["TimeStamp"]= str((int)(time.time()))       
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        taskScheduler.addToTaskQueue(TaskObject)
        Backend.batDect = 1        
    elif(msg["code"] == "5002"):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5002"     
        TaskDict["evCharge"]= "Off"
        TaskDict["TimeStamp"]= str((int)(time.time()))       
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        taskScheduler.addToTaskQueue(TaskObject)
        Backend.batDect = 0
    elif(msg["code"] == "5003"):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5003"
        TaskDict["chargeVal"]= str(round(Backend.chargeValue,2))        
        TaskDict["currAmpere"]= str(Backend.currAmpere)
        TaskDict["TimeStamp"]= str((int)(time.time()))
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        taskScheduler.addToTaskQueue(TaskObject)
    elif(msg["code"] == "5004"):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5004"  
        TaskDict["batDect"]= str(Backend.batDect)
        TaskDict["TimeStamp"]= str((int)(time.time()))
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        taskScheduler.addToTaskQueue(TaskObject)        
    else:
        print("Unknown code")
        
class tcpServerClient():
    clientsocket = None
    tempSocket = None    
    connectionType = ""
    def __init__(self,connectionType):
        tcpServerClient.connectionType = connectionType
        if(tcpServerClient.connectionType == "server"):
            print("Created tcpServer Instance")
        else:
            print("Created tcpClient Instance")    
        
    def createSocketConnection(self):
        try:
            # create a socket object
            tcpServerClient.tempSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
        except socket.error as err:
            print("Failed to create socket")
            print("Reason: %s",str(err))
            sys.exit()
        if(tcpServerClient.connectionType == "server"):    
            self.host = TCP_IP_HOST                           
            self.port = TCP_IP_PORT       
            # bind to the port
            tcpServerClient.tempSocket.bind((self.host, self.port))                                  
            # queue up to 5 requests
            tcpServerClient.tempSocket.listen(5) 
        print("Created Socket")

    def connect(self):
        tcpServerClient.clientsocket = tcpServerClient.tempSocket
        try:
            tcpServerClient.clientsocket.connect((TCP_IP_HOST,TCP_IP_PORT))
        except socket.error as err:	
            print("Failed to connect")
            print("Reason: %s",str(err))
            sys.exit()
      
    def waitForClientConnection(self):
        print("Waiting for Client connection")
        while True:
           # establish a connection
           tcpServerClient.clientsocket, self.addr = tcpServerClient.tempSocket.accept()
           print("Got a connection from %s" % str(self.addr))
           break
    
    def getTcpData(self):  
        print("Waiting for tcp data")
        while True:
            #sleep(0.1)
            if(tcpServerClient.clientsocket != None):
                data=tcpServerClient.clientsocket.recv(1024)
                if data:
                    #print(data.decode('utf-8'))
                    self.taskObj = json.loads(data.decode('utf-8'))
                    self.taskObj["commType"]= "tcp"
                    self.taskObj["transactionType"]= "rx"
                    #print(self.taskObj)
                    taskScheduler.addToTaskQueue(self.taskObj)                    
    @staticmethod
    def sendTcpData(data): 
        if(tcpServerClient.clientsocket != None):
            msg = json.dumps(data, indent=4)
            print(msg)
            tcpServerClient.clientsocket.send(msg.encode('ascii'))    

tcpCommInstance = tcpServerClient("server")
tcpCommInstance.createSocketConnection()
if(tcpServerClient.connectionType == "server"):
    #This is a blocked call and is waiting for the client to be connected
    tcpCommInstance.waitForClientConnection()
else:
    tcpCommInstance.connect()
    
# Start the thread once the socket connection is made
t2 = threading.Thread(target=tcpCommInstance.getTcpData)
t2.daemon = True
t2.start()
app = QGuiApplication(sys.argv)
beInstance = Backend()
app.exec_()


