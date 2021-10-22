import QtQuick 2.15
import QtQuick.Controls 2.15
import "uiBatteryComponent"

ApplicationWindow {
	visible: true
    width: 1920
    height: 1080
	flags: Qt.FramelessWindowHint | Qt.Window
	
    property QtObject backend
	property QtObject clockBackend
	property real chargeVal
	property bool batDisp
	property bool chargeOnOff
    property string currTime: "00:00:00"
	property string uiName
	property string uiEvId
	property string uiStartTime
	property string uiEndTime
	property real testVal: 0.1
	property bool batDect
	property bool batUnDect
	
    Rectangle {
        anchors.fill: parent
        Image {
            sourceSize.width: parent.width
            sourceSize.height: parent.height
            source: "background2.jpg"
            fillMode: Image.PreserveAspectCrop
        }		
    }

	Text 
	{
		id: title
		x:350
		y:50
		text: "GEEKS EV CHARGING STATION"
		font.family: "Helvetica"
		font.pointSize: 50
		font.bold: true
		color: "white"
	}

    UiBattery {
        id: battery
		x:150
		y:400
        anchors.left: parent
        anchors.top: parent
        anchors.topMargin: 50
        anchors.leftMargin: 50
        value: chargeVal
        charging: chargingToggle.checked
        maxLiquidRotation: 0
        rotation: -90
		visible: batDisp
    }

	Text 
	{
		id: plugMsg
		x:150
		y:350
		text: "Connect to \ncharging \npoint"
		font.family: "Helvetica"
		font.pointSize: 40
		color: "yellow"
		font.bold: true
		visible: !batDisp
		wrapMode: Text.WordWrap
	}


	
	Image 
	{
        id: connectImage
		x:300
		y:650
		width: 300
		height: 300
		source: "connected.png"
		fillMode: Image.PreserveAspectCrop
		visible: batDect
    }

	Image 
	{
        id: disconnectImage
		x:300
		y:650
		width: 300
		height: 300
		source: "notConnected.png"
		fillMode: Image.PreserveAspectCrop
		visible: batUnDect
    }
	
	SequentialAnimation{
		NumberAnimation{
		target: battery; properties: "scale"
		from: 1.0; to: 0.5; duration: 500
		}
		NumberAnimation{
		target: battery; properties: "opacity"
		from: 1.0; to: 0.2; duration: 500
		}
		NumberAnimation{
		target: battery; properties: "opacity"
		from: 0.2; to: 1.0; duration: 500
		}
		NumberAnimation{
		target: battery; properties: "scale"
		from: 0.5; to: 1.0; duration: 500
		}
		running: true		
	}
	
	Image 
	{
        id: qrCodeImage
		width: 400
		height: 400
		source: "deviceQR.png"
		fillMode: Image.PreserveAspectCrop
		anchors.centerIn: parent
    }
		
    NumberAnimation {
        id: rotateBackAnimation
        target: battery
        property: "rotation"
        to: -90
        duration: 3000
        easing.type: Easing.OutElastic
    }
	
	Text 
	{
		id: scan
		x:1250
		y:370
		text: "Scan the QR code \n to connect \nto the Station"
		font.family: "Helvetica"
		font.pointSize: 40
		font.bold: true
		color: "green"
		wrapMode: Text.WordWrap
		visible: !batDisp
	}


    Rectangle {
		id: column1
		x:1250
		y:370
		visible: batDisp
		
		Row
		{
			id: row1
			anchors.top: column1.top
			anchors.topMargin: 50
			
			spacing: 30
			Text 
			{
				id: username1
				text: "User Name :"
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "white"
			}
			Text 
			{
				id: username
				text: uiName
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "red"
			}		
		}

		Row
		{
			id: row2
			anchors.top: column1.top
			anchors.topMargin: 100			
			spacing: 30		
			Text 
			{
				anchors.top: username1.bottom
				id: evNumber1
				text: "Vehicle Number:"
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "white"
			}
			
			Text 
			{
				id: evNumber
				text: uiEvId
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "red"
			}
		}
		
		Row
		{
			id: row3
			anchors.top: column1.top
			anchors.topMargin: 150			
			spacing: 30
			Text 
			{
				anchors.top: evNumber1.bottom
				id: startTime1
				text: "Start Time:"
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "white"
			}
			
			Text 
			{
				id: startTime
				text: uiStartTime
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "red"
			}
		}

		Row
		{
			id: row4
			anchors.top: column1.top
			anchors.topMargin: 200			
			spacing: 30		
			Text 
			{
				anchors.top: startTime1.bottom
				id: endTime1
				text: "End Time :"
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "white"
			}
			Text 
			{
				id: endTime
				text: uiEndTime
				font.family: "Helvetica"
				font.pointSize: 20
				font.bold: true
				color: "red"
			}
		}		
    }

    Row {
        anchors.horizontalCenter: battery.horizontalCenter
        anchors.bottom: battery.bottom
        anchors.bottomMargin: -300
        spacing: 16
        ToggleButton {
            id: chargingToggle
            icon: "plug.png"
			scale: 1.5
			checked: chargeOnOff
			visible: batDisp			
        }
    }
}