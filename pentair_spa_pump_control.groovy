/**
 *  Copyright 2019 Brad Sileo
 *
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pentair Spa Pump Control", 
        namespace: "bsileo", 
        author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_spa_pump_control.groovy') {
	capability "Switch"
        command "onConfirmed"
        command "offConfirmed"
        attribute "friendlyName", "string"
        attribute "circuitFunction", "string"
	}
}

def installed() {	
}

def parse(String description) {
	try {
         def pair = description.split(":")
         createEvent(name: pair[0].trim(), value: pair[1].trim())
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
	parent.spaPumpOn()
    sendEvent(name: "switch", value: "turningOn", displayed:false,isStateChange:false)    
}

def off() {
	parent.spaPumpOff()
    sendEvent(name: "switch", value: "turningOff", displayed:false,isStateChange:false)
}

def setFriendlyName(name) {
   //log.debug("Set FName to ${name}")
   sendEvent(name: "friendlyName", value: name, displayed:false)
}

def setCircuitFunction(name) {
   //log.debug("Set CircuitFunction to ${name}")
   sendEvent(name: "circuitFunction", value: name, displayed:false)
}