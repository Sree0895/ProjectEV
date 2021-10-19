#!/usr/bin/env python
import sys
import os
import pyqrcode
#import QUrl
from PyQt5.QtGui import QGuiApplication
from PyQt5.QtQml import QQmlApplicationEngine
#from PyQt5.QtQuickView import QQuickView
from PyQt5.QtQuick import QQuickView
from PyQt5.QtCore import QUrl, QTimer, pyqtSignal, QObject
from time import strftime, localtime
import json
import paho.mqtt.client as mqtt
import socket
import sys
import threading
import time
from time import sleep
import queue


TCP_IP_HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
TCP_IP_PORT = 9999         # Port to listen on (non-privileged ports are > 1023)
MQTT_HOST = "192.168.1.34" # Standard loopback interface address (localhost)
MQTT_PORT = 1883           # Port to listen on (non-privileged ports are > 1023)

global evcsInfo      
global tcpServerInstance

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
            sleep(0.1)
            if(tcpServerClient.clientsocket != None):
                data=tcpServerClient.clientsocket.recv(1024)
                if data:
                    self.taskObj = json.loads(data.decode('utf-8'))
                    self.taskObj["commType"]= "tcp"
                    self.taskObj["transactionType"]= "rx"
                    taskScheduler.addToTaskQueue(self.taskObj)                    
    @staticmethod
    def sendTcpData(data): 
        if(tcpServerClient.clientsocket != None):
            msg = json.dumps(data, indent=4)
            #print(msg)
            tcpServerClient.clientsocket.send(msg.encode('ascii'))    
            
class UserInfo():
    def __init__(self):
        self.userName = ""
        self.evNumber = ""
        self.evChargeControl = "" 
        self.initialCharge = ""
        self.finalCharge = ""
        self.startTime = ""
        self.endTime = ""
        self.evChargeStatus = "Unknown"
        self.timeOut = 8
    def setUserName(self,name):
        self.userName = name
    def getUserName(self):
        return self.userName        
    def setEvNumber(self,evNumber):
        self.evNumber = evNumber
    def getEvNumber(self):
        return self.evNumber        
    def setInitialCharge(self,initialCharge):
        self.initialCharge = initialCharge
    def getInitialCharge(self):
        return self.initialCharge        
    def setFinalCharge(self,finalCharge):
        self.finalCharge = finalCharge
    def getFinalCharge(self):
        return self.finalCharge        
    def setStartTime(self,startTime):
        self.startTime = startTime
    def getStartTime(self):
        return self.startTime        
    def setEndTime(self,endTime):
        self.endTime = endTime
    def getEndTime(self):
        return self.endTime        
    def getTimeoutStatus(self):
        self.timeOut = self.timeOut - 1
        return (self.timeOut)        
    def resetData(self):
        self.userName = ""
        self.evNumber = ""
        self.evChargeControl = "" 
        self.initialCharge = ""
        self.finalCharge = ""
        self.startTime = ""
        self.endTime = ""
        self.evChargeStatus = "Unknown"        
        self.timeOut = 8
        print("Cleared User Data")

class mqttComm():
    client = None
    def __init__(self):
        super().__init__()
        if(mqttComm.client == None):
            mqttComm.client = mqtt.Client()
            mqttComm.client.on_connect = self.on_connect
            mqttComm.client.on_message = self.on_message
            mqttComm.client.connected_flag = False
        
    def mqttConnect(self,host,port):
        mqttComm.client.connect(host, port, 60)
        mqttComm.client.loop_start()

    # The callback for when the client receives a CONNACK response from the server.
    def on_connect(self,client, userdata, flags, rc):
        print("Connected with result code "+str(rc))
        # Subscribing in on_connect() means that if we lose the connection and
        # reconnect then subscriptions will be renewed.
        if rc==0:
            mqttComm.client.connected_flag=True
            topic = "topic/userService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) + "/"+ str(evcsInfo["evcsId"]) + "/" + "#"
            mqttComm.client.subscribe(topic)
        else:
            mqttComm.client.connected_flag=False

    # The callback for when a PUBLISH message is received from the server.
    def on_message(self,client, userdata, msg):
        self.taskObj = json.loads(msg.payload)
        self.taskObj["commType"]= "mqtt"
        self.taskObj["transactionType"]= "rx"        
        taskScheduler.addToTaskQueue(self.taskObj)
        
    def mqttSend(self,data,topic):
        msg = json.dumps(data, indent = 4)
        mqttComm.client.publish(topic,msg)
        
def BackendParser(msg):
    if(msg["code"] == "101"):
        Backend.userName = msg["user"]
        Backend.evNumber = msg["evNumber"]
        Backend.evChargeControl = "On"
        if(Backend.requestStatus == ""):
            Backend.requestStatus = "new"
            Backend.terminationType = "unknown"
            Backend.stateOfCharge = -1
    elif(msg["code"] == "102"):
        Backend.userName = msg["user"]
        Backend.evNumber = msg["evNumber"]
        Backend.evChargeControl = "Off"
        Backend.terminationType = "user"    
    elif(msg["code"] == "5001"):
        Backend.evChargeControl = msg["evCharge"]
    elif(msg["code"] == "5002"):
        Backend.evChargeControl = msg["evCharge"]
    elif(msg["code"] == "5003"):
        Backend.stateOfCharge = float(msg["soc"]) 
        Backend.batVolt = float(msg["batVolt"])
        Backend.avgCurr = float(msg["avgCurr"])
        Backend.remCapacity = float(msg["remCapacity"])
        Backend.fullCapacity = float(msg["fullCapacity"])
        Backend.avgPwr = float(msg["avgPwr"])
        Backend.stateOfHealth = float(msg["soh"])      
    elif(msg["code"] == "5004"):
        Backend.batDect = msg["batDect"]
    elif(msg["code"] == "3001"):
        Backend.authenticateRequest = msg["authReq"]        
        Backend.userName = msg["user"]
        Backend.evNumber = msg["evNumber"]
        #Backend.evChargerType = msg["evChargerType"]
        #Backend.evVehicleType = msg["evVehicleType"]
        Backend.evChargeOption = int( msg["evChargeOption"])
        Backend.evChargeOptionParam = int (msg["evChargeOptionParam"])
    else:
        print("Unknown code")
        
class taskScheduler():
    global qTaskList

    qTaskList = queue.Queue()     
    def __init__(self):
        super().__init__()
        self.tcpCon = tcpServerClient("client")
        self.mqttCon = mqttComm()
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
                    self.mqttCon.mqttSend(item,pubTopic)
                elif("rx" == item["transactionType"]):
                    BackendParser(item)
            elif("tcp" == item["commType"]):
                if("tx" == item["transactionType"]):
                    del item["commType"]
                    del item["transactionType"]
                    tcpServerClient.sendTcpData(item)
                elif("rx" == item["transactionType"]):
                    BackendParser(item)
            
class chargerControl():
    def __init__(self):
        super().__init__()
        self.scheduler = taskScheduler()
              
    def turnOnCharging(self):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5001"     
    
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

    def turnOffCharging(self):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5002"     
    
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

    def getChargingStatus(self):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5003"     
    
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

    def getBatDectStatus(self):
        TaskDict ={}
        TaskDict["commType"]= "tcp"
        TaskDict["transactionType"]= "tx"
        TaskDict["code"]= "5004"     
    
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
        
class Backend(QObject):
    updated = pyqtSignal(str, arguments=['time'])

    userName = ""
    evNumber = ""
    evChargeControl = "" 
    batDect = "0" 
    initialCharge = ""
    finalCharge = ""
    startTime = ""
    endTime = ""
    requestStatus = ""
    authenticateRequest = ""
    
    stateOfCharge = -1
    batVolt = -1
    avgCurr = -1
    remCapacity = -1
    fullCapacity = -1
    avgPwr = -1
    stateOfHealth = -1
 
    evChargerType = ""
    evVehicleType = ""
    evChargeOption = -1
    evChargeOptionParam = -1 
    evChargeTime  = 0
    terminationType = "unknown"
    
    def __init__(self):
        super().__init__()
        # Define timer.
        self.currUserInfo = UserInfo()
        self.scheduler = taskScheduler()
        self.chargerControlInstance = chargerControl()
        self.stateOfCharge = 0.0
        self.timer = QTimer()
        self.timer.setInterval(1000)  # msecs 100 = 1/10th sec
        self.timer.timeout.connect(self.update_time)

        self.userName = ""
        self.evNumber = ""
        self.evChargeControl = ""   
        self.conDect = False
        self.conUnDect = False        

        self.timer2 = QTimer()
        self.timer2.setInterval(20)  # msecs 100 = 1/10th sec
        self.timer2.timeout.connect(self.executeTask)

    def resetBEParams(self):
        Backend.userName = ""
        Backend.evNumber = ""
        Backend.evChargeControl = "" 
        Backend.batDect = "0" 
        Backend.initialCharge = ""
        Backend.finalCharge = ""
        Backend.startTime = ""
        Backend.endTime = ""
        Backend.requestStatus = ""
        Backend.authenticateRequest = ""
        
        Backend.stateOfCharge = -1
        Backend.batVolt = -1
        Backend.avgCurr = -1
        Backend.remCapacity = -1
        Backend.fullCapacity = -1
        Backend.avgPwr = -1
        Backend.stateOfHealth = -1
     
        Backend.evChargerType = ""
        Backend.evVehicleType = ""
        Backend.evChargeOption = -1
        Backend.evChargeOptionParam = -1 
        Backend.evChargeTime  = 0
        Backend.terminationType = "unknown"
    
    def startTimers(self):
        self.timer.start()    
        self.timer2.start()
        
    def executeTask(self):    
         # Execute the task
        self.scheduler.executeFromTaskQueue()       

    def update_time(self):
        # Pass the current time to QML
        curr_time = strftime("%H:%M:%S", localtime())
        self.updated.emit(curr_time)
              
        self.periodicTcpData()
        
        if(Backend.authenticateRequest == "true"):
            Backend.authenticateRequest = ""
            self.userName = Backend.userName
            self.authResp()

        if(Backend.stateOfCharge != -1):
            self.stateOfCharge = Backend.stateOfCharge
            if(Backend.requestStatus == "new"):
                self.userName = Backend.userName
                self.evNumber = Backend.evNumber
                self.recordUserInfo()
                self.chargerControlInstance.turnOnCharging()
        
        if( (Backend.requestStatus != "inProcess") and (Backend.requestStatus != "completed")):
            if( Backend.batDect == "1" ):
                self.conDect = ~self.conDect 
                self.displayBatteryConnect(self.conDect)
            else:    
                self.conUnDect = ~self.conUnDect 
                self.displayBatteryDisconnect(self.conUnDect)            
        
        if( (Backend.requestStatus == "new") and (Backend.batDect == "1") ):
            #self.chargerControlInstance.turnOnCharging()
            self.currUserInfo.setInitialCharge(self.stateOfCharge)
            Backend.requestStatus = "inProcess"
            
        if(Backend.requestStatus == "inProcess"):
            self.evChargeControl = Backend.evChargeControl
            self.displayBatteryConnect(False)
            self.displayBatteryDisconnect(False)
            if(self.evChargeControl == "On"):
                self.displayBattery(True)
                self.setChargingStatus(True)
                self.setBatteryLevel(self.stateOfCharge)
                self.currUserInfo.setEndTime("----")         
                if( self.checkForChargeCompletion()):
                    self.evChargeControl = "Off"
                    Backend.evChargeControl = "Off"
            elif(self.evChargeControl == "Off"):
                self.currUserInfo.setFinalCharge(self.stateOfCharge)
                self.setChargingStatus(False)
                self.setBatteryLevel(self.stateOfCharge)
                self.currUserInfo.setEndTime(strftime("%H:%M:%S", localtime()))
                self.evChargeControl = "Unknown"
                self.chargerControlInstance.turnOffCharging()
                Backend.requestStatus = "completed"
            self.periodicUserData()
        
        if(Backend.requestStatus == "completed"):    
            if( 0 == self.currUserInfo.getTimeoutStatus() ):
                self.sendChargeTerminationMsg()
                self.currUserInfo.resetData()
                self.displayBattery(False) 
                Backend.requestStatus = ""
                Backend.stateOfCharge = -1
                Backend.evChargeTime = 0
                self.resetBEParams()
        self.dispStartTime(self.currUserInfo.getStartTime())
        self.dispEndTime(self.currUserInfo.getEndTime())        
        self.dispUserName(self.currUserInfo.userName)      
        self.dispEvNumber(self.currUserInfo.evNumber)                

    def checkForChargeCompletion(self):
        ret = False
        if( round(self.stateOfCharge,2) >= round(1.0,2)):
            ret = True
            Backend.terminationType = "normal"
        if(Backend.evChargeOption == 1):
            Backend.evChargeTime  = Backend.evChargeTime + 1
            if( Backend.evChargeTime >= Backend.evChargeOptionParam):
                Backend.terminationType = "normal"
                ret = True
        elif(Backend.evChargeOption == 2):    
            if( round(self.stateOfCharge,2) >= round((float(Backend.evChargeOptionParam)/100.0),2)):
                Backend.terminationType = "normal"
                ret = True
        return ret

    def authResp(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/userData/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) + "/"+ str(evcsInfo["evcsId"]) + "/" + str(self.userName) 
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "3002"
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
        
    def periodicUserData(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/userData/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) + "/"+ str(evcsInfo["evcsId"]) + "/" + str(self.currUserInfo.getUserName()) 
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "3003"
        TaskDict["user"]= self.currUserInfo.getUserName()
        TaskDict["evNumber"]= self.currUserInfo.getEvNumber()
        TaskDict["currCharge"]= self.stateOfCharge
        TaskDict["initialCharge"]= self.currUserInfo.getInitialCharge()
        TaskDict["finalCharge"]= self.currUserInfo.getFinalCharge()
        TaskDict["startTime"]= self.currUserInfo.getStartTime()
        TaskDict["endTime"]= self.currUserInfo.getEndTime()
        TaskDict["termination"]= Backend.terminationType
        TaskDict["evChargeOption"]= Backend.evChargeOption
        TaskDict["evChargeOptionParam"]= Backend.evChargeOptionParam        
                
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

    def sendChargeTerminationMsg(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/userData/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) + "/"+ str(evcsInfo["evcsId"]) + "/" + str(self.currUserInfo.getUserName()) 
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "3005"
        TaskDict["user"]= self.currUserInfo.getUserName()
        TaskDict["evNumber"]= self.currUserInfo.getEvNumber()
        TaskDict["currCharge"]= self.stateOfCharge
        TaskDict["initialCharge"]= self.currUserInfo.getInitialCharge()
        TaskDict["finalCharge"]= self.currUserInfo.getFinalCharge()
        TaskDict["startTime"]= self.currUserInfo.getStartTime()
        TaskDict["endTime"]= self.currUserInfo.getEndTime()
        TaskDict["termination"]= Backend.terminationType
        TaskDict["evChargeOption"]= Backend.evChargeOption
        TaskDict["evChargeOptionParam"]= Backend.evChargeOptionParam        
                
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
        
    def periodicTcpData(self):
        #if(Backend.batDect == "1"):
        self.chargerControlInstance.getChargingStatus()
        self.chargerControlInstance.getBatDectStatus()

    def displayBatteryConnect(self,status):
        engine.rootObjects()[0].setProperty("batDect", status)

    def displayBatteryDisconnect(self,status):
        engine.rootObjects()[0].setProperty("batUnDect", status)
       
    def displayBattery(self,status):
        engine.rootObjects()[0].setProperty("batDisp", status)
        
    def setBatteryLevel(self,batLevel):
        engine.rootObjects()[0].setProperty("chargeVal", batLevel)
        
    def setChargingStatus(self,status):
        engine.rootObjects()[0].setProperty("chargeOnOff", status)

    def dispUserName(self,userName):
        engine.rootObjects()[0].setProperty("uiName", userName)

    def dispEvNumber(self,evNumber):
        engine.rootObjects()[0].setProperty("uiEvId", evNumber)

    def dispStartTime(self,startTime):
        engine.rootObjects()[0].setProperty("uiStartTime", startTime)

    def dispEndTime(self,endTime):
        engine.rootObjects()[0].setProperty("uiEndTime", endTime)

    def recordUserInfo(self):
        self.currUserInfo.setUserName(Backend.userName)
        self.currUserInfo.setEvNumber(Backend.evNumber)
        self.currUserInfo.setStartTime(strftime("%H:%M:%S", localtime()))      
            
tcpCommInstance = tcpServerClient("client")
tcpCommInstance.createSocketConnection()
if(tcpServerClient.connectionType == "server"):
    #This is a blocked call and is waiting for the client to be connected
    tcpCommInstance.waitForClientConnection()
else:
    tcpCommInstance.connect()
    
# Start the thread once the socket connection is made
t2 = threading.Thread(target=tcpCommInstance.getTcpData)
t2.start()
            
mqttCommInstance = mqttComm()
mqttConnectThread = threading.Thread(target=mqttCommInstance.mqttConnect,args=(MQTT_HOST, MQTT_PORT))
mqttConnectThread.daemon = True
mqttConnectThread.start()

# Read the json file to get input required for QR code generation
f = open ('sample.json', "r")
evcsInfo = json.loads(f.read())

evData = json.dumps(evcsInfo )
# Create the QR code from the json data read
url = pyqrcode.create(evData)
url.png('deviceQR.png',scale = 6)

app = QGuiApplication(sys.argv)
engine = QQmlApplicationEngine()
engine.quit.connect(app.quit)
engine.load('main.qml')
beInstance = Backend()

beInstance.displayBattery(False)
beInstance.setBatteryLevel(beInstance.stateOfCharge)
beInstance.setChargingStatus(False)
beInstance.dispUserName("")
beInstance.dispEvNumber("")
beInstance.dispStartTime("")
beInstance.dispEndTime("")
beInstance.displayBatteryConnect(False)
beInstance.displayBatteryDisconnect(False)
beInstance.startTimers()

sys.exit(app.exec())

