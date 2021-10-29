#!/usr/bin/env python3
import sys
import os
import pyqrcode
from PyQt5.QtGui import QGuiApplication
from PyQt5.QtQml import QQmlApplicationEngine
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
import logging
import pymongo
import datetime

TCP_IP_HOST = '127.0.0.1'  # Standard loopback interface address (localhost)
TCP_IP_PORT = 9999         # Port to listen on (non-privileged ports are > 1023)
MQTT_HOST = "192.168.1.34" # Standard loopback interface address (localhost)
MQTT_PORT = 1883           # Port to listen on (non-privileged ports are > 1023)
MONGO_SERVER = "mongodb://localhost:27017/"
MONGO_DATABASE_NAME = "bookingDataBase"
MONGO_DATABASE_COLLECTION = "bookingCollection"
SIMULATE_OTHER_STATIONS = False

mongClient = pymongo.MongoClient(MONGO_SERVER)
mongDB = mongClient[MONGO_DATABASE_NAME]
mongCollection = mongDB[MONGO_DATABASE_COLLECTION]

logging.basicConfig(filename="uiControl.log",
                    format='%(asctime)s %(message)s',
                    filemode='w')
  
#Creating an object
logger=logging.getLogger()
  
#Setting the threshold of logger to DEBUG
logger.setLevel(logging.DEBUG)

global evcsInfo      
global tcpServerInstance
global evcsStatus
global subBookingSlotReqTopic
global subUserServiceTopic
global pubUserServiceTopic
global pubBookingSlotServiceTopic
global pubLocationServiceTopic


def set_bit(value, bit):
    return value | (1<<bit)

def clear_bit(value, bit):
    return value & ~(1<<bit)
    
   
def getFreeSlotStatus():
    slotList = 0
    slotCount = 0
    for slot in range(0,24):
        query = { "slot": { "$regex": "^" + str(slot)  +"$"} }
        listAr = list((mongCollection.find(query,{"_id":0})))
        jsonObj = json.loads(json.dumps(listAr[0]))
        if(jsonObj["user"] == "null"):
            slotList = set_bit(slotList,slot)
            slotCount = slotCount + 1
            
    return slotList,slotCount     
        
def getCurrentSlotInfo():
    currDateTime = datetime.datetime.now()
    currHour = currDateTime.strftime("%H")
    slot=str(currHour)
    query = { "slot": { "$regex": "^" + slot  +"$"} }

    listAr = list((mongCollection.find(query,{"_id":0})))
    jsonObj = json.loads(json.dumps(listAr[0]))
    
    if(jsonObj["user"] != "null"):
        result = "Allocated to " + jsonObj["user"] + ":"+ jsonObj["evNumber"]
    else:
        result = "Available"
    return result
    
def getNextSlotInfo():
    currDateTime = datetime.datetime.now()
    currHour = int (currDateTime.strftime("%H"))
    currHour = currHour + 1
    currHour = currHour%24

    slot=str(currHour)
    query = { "slot": { "$regex": "^" + slot  +"$"} }

    listAr = list((mongCollection.find(query,{"_id":0})))
    jsonObj = json.loads(json.dumps(listAr[0]))
    
    if(jsonObj["user"] != "null"):
        result = "Allocated to " + jsonObj["user"] + ":"+ jsonObj["evNumber"]
    else:
        result = "Available"
    return result
    
def getBookingSlotStatus(slot,user,evNumber):
    status = ""
    currDateTime = datetime.datetime.now()
    
    evcsManufacturer= str(evcsInfo["evcsManufacturer"])
    evcsState= str(evcsInfo["evcsState"])
    evcsDistrict= str(evcsInfo["evcsDistrict"])
    day=str(currDateTime.strftime("%d"))
    month=str(currDateTime.strftime("%b"))
    year= str(currDateTime.year)

    query1 = { "$and": [{ "slot": { "$regex": "^" + slot  +"$"} }, { "user": { "$regex": "null" }}]}
    query2 = { "slot": { "$regex": "^" + slot  +"$"} }
    query3 = { "$and": [{ "slot": { "$regex": "^" + slot  +"$"} }, { "user": { "$regex": "^" + user  +"$"}}]}
    newvalues = { "$set": { "user" : user,"evNumber" : evNumber,"evcsManufacturer": evcsManufacturer,"evcsState": evcsState,"evcsDistrict": evcsDistrict,"day":day, "month":month, "year":year} }

    #Check if the booking already exists for the given slot
    docResult0 = list (mongCollection.find(query3))
    if(len(docResult0) == 0 ):
        docResult1 = list (mongCollection.find(query1))
        if(len(docResult1) != 0 ): 
            mongCollection.update_one(query2,newvalues)
            docResult2 = list (mongCollection.find(query3))
            if(len(docResult2) != 0 ):
                status="Booking Successful"
            else:    
                status="Try again"
        else:
            status= "Slot not available"
    else:
        status ="Booking already done" 
    return status

def getReleaseSlotStatus(slot,user):
    nullVal = "null"
    status = ""
    
    query1 = { "$and": [{ "slot": { "$regex": "^" + slot  +"$"} }, { "user": { "$regex": "null" }}]}
    query2 = { "slot": { "$regex": "^" + slot  +"$"} }
    query3 = { "$and": [{ "slot": { "$regex": "^" + slot  +"$"} }, { "user": { "$regex": "^" + user  +"$"}}]}
    newvalues = { "$set": { "user" : nullVal,"evNumber" : nullVal,"evcsManufacturer": nullVal,"evcsState": nullVal,"evcsDistrict": nullVal,"day":nullVal, "month":nullVal, "year":nullVal} }

    #Check if the booking already exists for the given slot
    docResult0 = list (mongCollection.find(query3))
    if(len(docResult0) != 0 ):
        mongCollection.update_one(query2,newvalues)
        docResult2 = list (mongCollection.find(query1))
        if(len(docResult2) != 0 ):
            status ="Release successful"
        else:
            status ="Release unsuccessful"       
    else:
        status ="Booking does not exist" 
        
    return status    
    
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
                            print("TCP data exception")
                        finally:
                            pass
            except ConnectionResetError:
                print("TCP connection Error")
                tcpServerClient.clientsocket.close()
                tcpServerClient.clientsocket = None
                #self.connect()
            finally:
                pass                            
    @staticmethod
    def sendTcpData(data): 
        if(tcpServerClient.clientsocket != None):
            try:
                msg = json.dumps(data, indent=4)
                tcpServerClient.clientsocket.send(msg.encode('ascii'))
            except:
                pass
            finally:
                pass
    
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
            subUserServiceTopic = "topic/userService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) + "/"+ str(evcsInfo["evcsId"]) + "/" + "#"
            subBookingSlotReqTopic = "topic/bookingSlotRequest/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) 
            mqttComm.client.subscribe([(subUserServiceTopic, 0), (subBookingSlotReqTopic, 0)])
        else:
            mqttComm.client.connected_flag=False

    # The callback for when a PUBLISH message is received from the server.
    def on_message(self,client, userdata, msg):
        self.taskObj = json.loads(msg.payload )
        self.taskObj["commType"]= "mqtt"
        self.taskObj["transactionType"]= "rx"        
        taskScheduler.addToTaskQueue(self.taskObj)
        
    def mqttSend(self,data,topic):
        msg = json.dumps(data, indent = 4)
        logger.debug(msg)
        mqttComm.client.publish(topic,msg)
        
def BackendParser(msg):
    try:
        if(msg["code"] == "101"):
            Backend.userName = msg["user"]
            Backend.evNumber = msg["evNumber"]
            Backend.evChargeControl = "On"
            if(Backend.requestStatus == ""):
                Backend.requestStatus = "new"
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
            Backend.evChargerType = msg["evChargerType"]
            Backend.evVehicleType = msg["evVehicleType"]
            Backend.evChargeOption = int( msg["evChargeOption"])
            Backend.evChargeOptionParam = int (msg["evChargeOptionParam"])
        elif(msg["code"] == "9002"):
            Backend.bookingRequest = True        
            Backend.bookingUserName = msg["user"]
            Backend.bookingEvNumber = msg["evNumber"]
            Backend.bookingEvChargerType = msg["evChargerType"]
            Backend.bookingEvVehicleType = msg["evVehicleType"]
            Backend.bookingSlotReq = int(msg["slotReq"])
        elif(msg["code"] == "9004"):
            Backend.bookingRequest = False        
            Backend.bookingUserName = msg["user"]
            Backend.bookingEvNumber = msg["evNumber"]
            Backend.bookingEvChargerType = msg["evChargerType"]
            Backend.bookingEvVehicleType = msg["evVehicleType"]
            Backend.bookingSlotReq = int(msg["slotReq"])            
        else:
            print("Unknown code")
    except:
        print("exception")
    finally:
        pass
        
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
    evChargingCost = 50.0
    
    bookingRequest = ""        
    bookingUserName = ""
    bookingEvNumber = ""
    bookingEvChargerType = ""
    bookingEvVehicleType = ""
    bookingSlotReq = -1        
    
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
        
        self.fiveSecTimerCnt = 0

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
        Backend.evChargingCost = 50.0
        
        Backend.bookingRequest = ""        
        Backend.bookingUserName = ""
        Backend.bookingEvNumber = ""
        Backend.bookingEvChargerType = ""
        Backend.bookingEvVehicleType = ""
        Backend.bookingSlotReq = -1
            
    
    def startTimers(self):
        self.timer.start()    
        self.timer2.start()
        
    def executeTask(self):    
         # Execute the task
        self.scheduler.executeFromTaskQueue()       

    def update_time(self):
             
        self.periodicTcpData()
        self.updateSlotDisplay()
        
        if(self.fiveSecTimerCnt%5 == 0):
            self.sendEvStatusMsg()
            self.sendLocationServiceMsg()
            self.sendBookingServiceMsg()
            self.sendSubBookingServiceMsg()
        self.fiveSecTimerCnt = self.fiveSecTimerCnt + 1   
        
        if(Backend.bookingRequest != ""):
            self.processBookingRequest()
            Backend.bookingRequest = ""
        
        if(Backend.authenticateRequest == "true"):
            Backend.authenticateRequest = "false"
            self.userName = Backend.userName
            self.authResp()

        if(Backend.authenticateRequest == "false"):
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
                self.currUserInfo.setInitialCharge(self.stateOfCharge)
                Backend.requestStatus = "inProcess"
                
            if(Backend.requestStatus == "inProcess"):
                self.evChargeControl = Backend.evChargeControl
                self.displayBatteryConnect(False)
                self.displayBatteryDisconnect(False)
                if(self.evChargeControl == "On"):
                    Backend.evChargingCost = Backend.evChargingCost + 0.15
                    self.dispCost( "Rs. "+ str(round(Backend.evChargingCost,2)) )
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
                    self.resetBEParams()
            self.dispStartTime(self.currUserInfo.getStartTime())
            self.dispEndTime(self.currUserInfo.getEndTime())        
            self.dispUserName(self.currUserInfo.userName)      
            self.dispEvNumber(self.currUserInfo.evNumber)
            
        if(Backend.authenticateRequest == ""):    
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
        TaskDict["user"]= Backend.userName
        TaskDict["evNumber"]= Backend.evNumber
        TaskDict["evChargeOption"]= Backend.evChargeOption
        TaskDict["evChargeOptionParam"]= Backend.evChargeOptionParam
        TaskDict["TimeStamp"]= str((int)(time.time()))
        
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
        TaskDict["evChargingCost"]= round(Backend.evChargingCost,2)
        TaskDict["TimeStamp"]= str((int)(time.time()))        
                
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
        TaskDict["evChargingCost"]= Backend.evChargingCost
        TaskDict["TimeStamp"]= str((int)(time.time()))        
                
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

    def sendLocationServiceMsg(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/locationService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"])
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "1001"
        TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
        TaskDict["evcsState"]= str(evcsInfo["evcsState"])
        TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
        TaskDict["evcsName"]= str(evcsInfo["evcsName"])
        TaskDict["evcsId"]= str(evcsInfo["evcsId"])     
        TaskDict["evcsLat"]= str(evcsInfo["evcsLat"])
        TaskDict["evcsLon"]= str(evcsInfo["evcsLon"])
        TaskDict["TimeStamp"]= str((int)(time.time()))
            
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
        
        if(SIMULATE_OTHER_STATIONS == True):
            TaskDict ={}
            TaskDict["commType"]= "mqtt"
            TaskDict["transactionType"]= "tx"
            pubTopic = "topic/locationService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"])
            TaskDict["topic"]= pubTopic
            TaskDict["code"]= "1001"
            TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
            TaskDict["evcsState"]= str(evcsInfo["evcsState"])
            TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
            TaskDict["evcsName"]= "Station 2 - Gachibowli"
            TaskDict["evcsId"]= "EV002378"    
            TaskDict["evcsLat"]= "17.4401"
            TaskDict["evcsLon"]= "78.3489"
            TaskDict["TimeStamp"]= str((int)(time.time()))
                
            TaskStr = json.dumps(TaskDict, indent = 4)
            TaskObject = json.loads(TaskStr)
            self.scheduler.addToTaskQueue(TaskObject)        

    def sendBookingServiceMsg(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/bookingSlotService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"])
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "9001"
        TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
        TaskDict["evcsState"]= str(evcsInfo["evcsState"])
        TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
        TaskDict["evcsName"]= str(evcsInfo["evcsName"])
        TaskDict["evcsId"]= str(evcsInfo["evcsId"])     
        TaskDict["evcsLat"]= str(evcsInfo["evcsLat"])
        TaskDict["evcsLon"]= str(evcsInfo["evcsLon"])
        TaskDict["TimeStamp"]= str((int)(time.time()))
            
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)

        if(SIMULATE_OTHER_STATIONS == True):
            TaskDict ={}
            TaskDict["commType"]= "mqtt"
            TaskDict["transactionType"]= "tx"
            pubTopic = "topic/bookingSlotService/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"])
            TaskDict["topic"]= pubTopic
            TaskDict["code"]= "9001"
            TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
            TaskDict["evcsState"]= str(evcsInfo["evcsState"])
            TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
            TaskDict["evcsName"]= "Station 2 - Gachibowli"
            TaskDict["evcsId"]= "EV002378"   
            TaskDict["evcsLat"]= "17.4401"
            TaskDict["evcsLon"]= "78.3489" 
            TaskDict["TimeStamp"]= str((int)(time.time()))
            
            TaskStr = json.dumps(TaskDict, indent = 4)
            TaskObject = json.loads(TaskStr)
            self.scheduler.addToTaskQueue(TaskObject)
        
    def sendSubBookingServiceMsg(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/bookingSlotServiceEvcs/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"])
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "9000"
        TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
        TaskDict["evcsState"]= str(evcsInfo["evcsState"])
        TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
        TaskDict["evcsName"]= str(evcsInfo["evcsName"])
        TaskDict["evcsId"]= str(evcsInfo["evcsId"])     
        TaskDict["freeSlots"], slotCnt = getFreeSlotStatus()
        TaskDict["TimeStamp"]= str((int)(time.time()))
             
        self.dispFreeSlots(str(slotCnt) + "/24")     
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
                      
    def sendEvStatusMsg(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/cpoData/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"])
        TaskDict["topic"]= pubTopic
        TaskDict["code"]= "6001"
        TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
        TaskDict["evcsState"]= str(evcsInfo["evcsState"])
        TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
        TaskDict["evcsName"]= str(evcsInfo["evcsName"])
        TaskDict["maxSlots"]= str(1)
        TaskDict["TimeStamp"]= str((int)(time.time()))

        if(evcsStatus == "good"):
            if(Backend.requestStatus == "inProcess"):
                TaskDict["status"]= "busy" 
                TaskDict["freeSlots"]= str(0)    
            else:
                TaskDict["status"]= "free"
                TaskDict["freeSlots"]= str(1)
        else:        
            TaskDict["status"]= "unknown" 
            
        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
        
    def periodicTcpData(self):
        self.chargerControlInstance.getChargingStatus()
        self.chargerControlInstance.getBatDectStatus()

    def processBookingRequest(self):
        TaskDict ={}
        TaskDict["commType"]= "mqtt"
        TaskDict["transactionType"]= "tx"
        pubTopic = "topic/bookingSlotServiceEvcs/" + str(evcsInfo["evcsManufacturer"]) + "/" + str(evcsInfo["evcsState"]) + "/" + str(evcsInfo["evcsDistrict"]) + "/" + str(evcsInfo["evcsName"]) 
        TaskDict["topic"]= pubTopic
        
        slotId = findPosition(Backend.bookingSlotReq)
        if(slotId != -1):
            slotIdStr = str(slotId-1)
            
        if(Backend.bookingRequest == True):
            TaskDict["code"]= "9003"
            TaskDict["response"]= getBookingSlotStatus(slotIdStr,Backend.bookingUserName,Backend.bookingEvNumber)
        else:
            TaskDict["code"]= "9005"
            TaskDict["response"]= getReleaseSlotStatus(slotIdStr,Backend.bookingUserName)

        TaskDict["evcsManufacturer"]= str(evcsInfo["evcsManufacturer"])
        TaskDict["evcsState"]= str(evcsInfo["evcsState"])
        TaskDict["evcsDistrict"]= str(evcsInfo["evcsDistrict"])
        TaskDict["evcsName"]= str(evcsInfo["evcsName"])
        TaskDict["user"]= Backend.bookingUserName
        TaskDict["evNumber"]= Backend.bookingEvNumber
        TaskDict["TimeStamp"]= str((int)(time.time()))

        TaskStr = json.dumps(TaskDict, indent = 4)
        TaskObject = json.loads(TaskStr)
        self.scheduler.addToTaskQueue(TaskObject)
    
    def updateSlotDisplay(self):
        self.dispCurrTime()
        self.dispCurrSlot(getCurrentSlotInfo())
        self.dispNextSlot(getNextSlotInfo())
       
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

    def dispCurrTime(self):
        engine.rootObjects()[0].setProperty("uiCurrentTime", str(datetime.datetime.now().strftime("%H:%M:%S, %d %B %Y")))

    def dispCurrSlot(self,currSlot):
        engine.rootObjects()[0].setProperty("uiCurrentSlot", currSlot)

    def dispNextSlot(self,nexSlot):
        engine.rootObjects()[0].setProperty("uiNextSlot", nexSlot)
    
    def dispCost(self,cost):
        engine.rootObjects()[0].setProperty("uiCostVal", cost)

    def dispEvcsName(self,name):
        engine.rootObjects()[0].setProperty("uiEvcsName", name)

    def dispFreeSlots(self,slotCount):
        engine.rootObjects()[0].setProperty("uiFreeSlots", slotCount)
        
    def recordUserInfo(self):
        self.currUserInfo.setUserName(Backend.userName)
        self.currUserInfo.setEvNumber(Backend.evNumber)
        self.currUserInfo.setStartTime(strftime("%H:%M:%S", localtime()))      

# A utility function to check
# whether n is power of 2 or
# not.
def isPowerOfTwo(n):
    return (True if(n > 0 and
                   ((n & (n - 1)) > 0))
                 else False);
     
# Returns position of the
# only set bit in 'n'
def findPosition(n):
    if (isPowerOfTwo(n) == True):
        return -1;
 
    i = 1;
    pos = 1;
 
    # Iterate through bits of n
    # till we find a set bit i&n
    # will be non-zero only when
    # 'i' and 'n' have a set bit
    # at same position
    while ((i & n) == 0):
         
        # Unset current bit and
        # set the next bit in 'i'
        i = i << 1;
 
        # increment position
        pos += 1;
 
    return pos;
    
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

evcsStatus = "good"
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
beInstance.dispCurrTime()
beInstance.dispCurrSlot("---")
beInstance.dispNextSlot("---")
beInstance.dispCost("TBD")
beInstance.dispEvcsName(evcsInfo["evcsName"] + "," +evcsInfo["evcsDistrict"] +","+evcsInfo["evcsState"])
beInstance.dispFreeSlots("---")
    
beInstance.startTimers()

sys.exit(app.exec())

