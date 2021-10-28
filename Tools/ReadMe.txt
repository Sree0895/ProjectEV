
---------------------------------------------------------
PACKAGE INSTALLATION ON WINDOWS
---------------------------------------------------------

If python is not installed then install Python 3.8.10 in Windows 10 by using python-3.8.10-amd64.exe

After installing python, open the command prompt and create the virtual environment

python -m venv /path/to/new/virtual/environment

Example : python -m venv C:\pythonQt\env\qt_dev_env  --> Here the environment qt_dev_env will be created

Go to the virtual environment path and execute the activate.bat script.
 
C:\pythonQt\env\qt_dev_env\Scripts\activate.bat

Once the environment is activated, execute the command pip list
You should be seeing the following list of packages
Package    Version
---------- -------
pip        21.1.1
setuptools 56.0.0

We need paho-mqtt, pypng, pyQRcode and pyqt5 packages.

Execute the following commands to install the above packages.

pip install paho-mqtt
pip install pypng
pip install PyQRCode
pip install PyQt5
pip install pymongo

Once the required packages are installed, execute the command pip list to check the packages installed.
It should look like below.
 
Package    Version
---------- -------
paho-mqtt  1.5.1
pip        21.1.1
pymongo    3.12.1
pypng      0.0.21
PyQRCode   1.2.1
PyQt5      5.15.4
PyQt5-Qt5  5.15.2
PyQt5-sip  12.9.0	
setuptools 56.0.0

Download the MongoDB community server from the following link.
https://www.mongodb.com/try/download/community

Use the mongodb-windows-x86_64-5.0.3-signed.msi for installing mongoDB database server on Windows 10.
Refer the following link for installing the mongoDB and creating the database.
https://www.prisma.io/dataguide/mongodb/setting-up-a-local-mongodb-database

Specify the database name as bookingDataBase
Specify the collection name as bookingCollection
Select bookingCollection and Cick the "ADD DATA" button.
Choose the "Insert Document" option and enter the following content 

[
{ "slot" : "0" , "user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "1" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "2" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "3" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "4" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "5" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "6" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "7" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "8" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{ "slot" : "9" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "10" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "11" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "12" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "13" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "14" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "15" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "16" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "17" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "18" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "19" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "20" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "21" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "22" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"}, 
{  "slot" : "23" ,"user" : "null", "evNumber" : "null", "evcsManufacturer": "null","evcsState": "null","evcsDistrict": "null", "day":"null","month":"null","year":"null"} 
]

---------------------------------------------------------
PACKAGE INSTALLATION ON RASPBERRY PI
---------------------------------------------------------
Install Mosquitto mqtt broker on Raspberry pi.
Use the following commands to install it.
 
sudo apt-get update
sudo apt-get install mosquitto 

Refer the following link for more info if required
https://www.instructables.com/Installing-MQTT-BrokerMosquitto-on-Raspberry-Pi/

Install paho mqtt client by executing the following command
pip install paho-mqtt

Note,Mosquitto mqtt broker will not be required in the end application Since we would be using the AWS.

---------------------------------------------------------
EXECUTING PYTHON SCRIPTS
---------------------------------------------------------
The uiControl.py and batteryControl.py needs to be run in two different command prompts.
Activate the python environment as explained earlier
Execute the batteryControl.py first and then the uiControl.py.
In the uiControl.py, update the mqtt broker address (Here it is the ip address of the Raspberry pi)
In the script it i set as below:
MQTT_HOST = "192.168.1.34"

Similarly update the mqtt broker address in the mobile appplication
The address can be updated in the config.java file
private static String mHost = "192.168.1.34"; 