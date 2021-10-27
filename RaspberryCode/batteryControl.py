#!/usr/bin/env python3

import socket
import json
import time
import queue
import logging

from time import sleep
import sys
import os
import threading
import random

TCP_IP_HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
TCP_IP_PORT = 9999         # Port to listen on (non-privileged ports are > 1023)

class Backend():
    stateOfCharge = 0.0
    batVolt = 0.0
    avgCurr = 0.0
    remCapacity = 0.0
    fullCapacity = 0.0
    avgPwr = 0.0
    stateOfHealth = 0.0
    chargeValue = 0.1
    batDect=0
    def __init__(self):
        super().__init__()
        
        self.scheduler = taskScheduler()
        
    def update_time(self):
        if(Backend.batDect == 1):
            Backend.stateOfCharge= Backend.stateOfCharge+ 0.01
            if(Backend.stateOfCharge > 1.0):
                Backend.stateOfCharge = 1.0
            
    def executeTask(self):    
         # Execute the task
        self.scheduler.executeFromTaskQueue()
    
    @staticmethod
    def getStateOfCharge():
        return Backend.stateOfCharge
    @staticmethod    
    def getBatVoltage():
        return Backend.batVolt
    @staticmethod
    def getBatAvgCurrent():
        return Backend.avgCurr
    @staticmethod
    def getBatRemainingCapacity():
        return Backend.remCapacity
    @staticmethod
    def getBatFullCapacity():
        return Backend.fullCapacity
    @staticmethod
    def getBatAvgPower():
        return Backend.avgPwr
    @staticmethod
    def getStateOfHealth():
        return Backend.stateOfHealth
    @staticmethod
    def getBatDect():
        return Backend.batDect    
class taskScheduler():
    global qTaskList

    qTaskList = queue.Queue()     
    def __init__(self):
        self.tcpCon = tcpServerClient("server")
        print("Initiated Task Scheduler class")

    @staticmethod
    def addToTaskQueue(item):
        if not qTaskList.full():
            qTaskList.put(item )

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
        if(Backend.batDect == 0): 
            Backend.stateOfCharge = 0.05
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
        TaskDict["batDect"]= str(Backend.getBatDect())
        TaskDict["soc"]= str(round(Backend.getStateOfCharge(),2))
        TaskDict["batVolt"]= str(round(Backend.getBatVoltage(),2))        
        TaskDict["avgCurr"]= str(round(Backend.getBatAvgCurrent(),2))
        TaskDict["remCapacity"]= str(round(Backend.getBatRemainingCapacity(),2))
        TaskDict["fullCapacity"]= str(round(Backend.getBatFullCapacity(),2))
        TaskDict["avgPwr"]= str(round(Backend.getBatAvgPower(),2))
        TaskDict["soh"]= str(round(Backend.getStateOfHealth(),2))
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
            try:
                if(tcpServerClient.clientsocket != None):
                    data=tcpServerClient.clientsocket.recv(1024)
                    if data:
                        try:
                            tempMsg = data.decode('utf-8')
                            tempMsg.replace("\n", "")
                            self.taskObj = json.loads(tempMsg)
                            self.taskObj["commType"]= "tcp"
                            self.taskObj["transactionType"]= "rx"
                            taskScheduler.addToTaskQueue(self.taskObj)
                        except:
                            print("TCP Data exception")
                        finally:
                            pass
            except ConnectionResetError:
                print("TCP connection Error")
                tcpServerClient.clientsocket.close()
                tcpServerClient.clientsocket = None
                self.waitForClientConnection()
            finally:
                pass
    @staticmethod
    def sendTcpData(data): 
        if(tcpServerClient.clientsocket != None):
            msg = json.dumps(data, indent=4)
            tcpServerClient.clientsocket.send(msg.encode('ascii'))    

class PeriodicThread(object):
    """
    Python periodic Thread using Timer with instant cancellation
    """

    def __init__(self, callback=None, period=1, name=None, *args, **kwargs):
        self.name = name
        self.args = args
        self.kwargs = kwargs
        self.callback = callback
        self.period = period
        self.stop = False
        self.current_timer = None
        self.schedule_lock = threading.Lock()

    def start(self):
        """
        Mimics Thread standard start method
        """
        self.schedule_timer()

    def run(self):
        """
        By default run callback. Override it if you want to use inheritance
        """
        if self.callback is not None:
            self.callback()

    def _run(self):
        """
        Run desired callback and then reschedule Timer (if thread is not stopped)
        """
        try:
            self.run()
        except:
            logging.exception("Exception in running periodic thread")
        finally:
            with self.schedule_lock:
                if not self.stop:
                    self.schedule_timer()

    def schedule_timer(self):
        """
        Schedules next Timer run
        """
        self.current_timer = threading.Timer(self.period, self._run, *self.args, **self.kwargs)
        if self.name:
            self.current_timer.name = self.name
        self.current_timer.start()

    def cancel(self):
        """
        Mimics Timer standard cancel method
        """
        with self.schedule_lock:
            self.stop = True
            if self.current_timer is not None:
                self.current_timer.cancel()

    def join(self):
        """
        Mimics Thread standard join method
        """
        self.current_timer.join()
    
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
beInstance = Backend()
periodicTask1 = PeriodicThread(beInstance.update_time, 3  )
periodicTask1.schedule_timer()
periodicTask2 = PeriodicThread(beInstance.executeTask, 0.1  )
periodicTask2.schedule_timer()

