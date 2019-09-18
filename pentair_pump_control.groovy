/**
 *  Copyright 2019 Brad Sileo
 *
 *  Author: Brad Sileo
 *
 *  Date: 2019-09-18
 */
metadata {
	definition (name: "Pentair Pump Control", 
            namespace: "bsileo", 
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_pump_control.groovy') {
	    capability "Switch"
        capability "Switch Level"
        capability "Power Meter"
        command "onConfirmed"
        command "offConfirmed"
        attribute "friendlyName", "string"
        attribute "circuitID","string"
        attribute "pumpID","string"
        attribute "rpm", "number"
        attribute "programLevel", "number"
        
	}
    
    preferences {
 		input (name: "maxRPM", type: "number", title: "Pump Maximum RPMs", default: 3500)
 	}
}


def installed() {
	log.debug("Installed Pump Control " + device.deviceNetworkId)
    manageData()
    manageChildren()       	
}

def updated() {
  manageData()
  manageChildren()
}

def manageChildren() {
}

def manageData() {
 	def cid = getDataValue("circuitID")
	sendEvent(name: "circuitID", value: cid, isStateChange: true, displayed: false)
    def pid = getDataValue("pumpID")
	sendEvent(name: "pumpID", value: pid, isStateChange: true, displayed: false)
    def name = getDataValue("friendlyName")
	sendEvent(name: "friendlyName", value: name, isStateChange: true, displayed: false)
}

def parsePumpData(pumpInfo) {
	log.debug("Pump Parse--${pumpInfo}")
  
    try {
         def programMode = pumpInfo['currentrunning']?.mode
         def programDuration = pumpInfo['currentrunning']?.remainingduration
         def programRPM = pumpInfo['currentrunning']?.value
         def friendlyName = pumpInfo.friendlyName
         def name = pumpInfo.name
         sendEvent(name: "friendlyName", value: friendlyName, isStateChange: true, displayed: false)         
         def mode = pumpInfo.mode
         def rpm = pumpInfo.rpm
         def level = (rpm / maxRPM)*100
         def status = rpm == 0 ? 'off' : 'on'
         sendEvent(name:'switch', value: status)
         sendEvent(name: 'level', value: level)
         sendEvent(name: 'rpm', value: rpm)
         sendEvent(name: 'programLevel', value: programRPM)
         def watts = pumpInfo.watts
         sendEvent(name: 'power', value: watts)
     }
     catch (java.lang.ArrayIndexOutOfBoundsException e) {
           log.debug "Error! " + e   
    }
	
}

def onConfirmed() {
    //log.debug("CONF ${device} turned on")
	sendEvent(name: "switch", value: "on", displayed:true)    
}

def offConfirmed() {
	//log.debug("CONF ${device} turned off")
	sendEvent(name: "switch", value: "off", displayed:true)  
}

def on() {
	parent.setCircuit(getDataValue("circuitID"), 1)
    sendEvent(name: "switch", value: "turningOn", displayed:false,isStateChange:false)    
}

def off() {
	parent.setCircuit(getDataValue("circuitID"), 0)
    sendEvent(name: "switch", value: "turningOff", displayed:false,isStateChange:false)
}

def setLevel(speed) {
	def pid = getDataValue("pumpID")
	log.debug("Pump Control ${pid} set speed to ${speed}")
	parent.setPumpSpeed(pid, speed)
    sendEvent(name: "rpmProgramRequest", value: speed, displayed:true,isStateChange:false)
}