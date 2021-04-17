/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller - Body
 *
 *  Author: Brad Sileo
 *
 *
 *  version: 0.9.13
 */

metadata {
	definition (name: "Pool Controller Body", namespace: "bsileo", author: "Brad Sileo") {

       capability "Refresh"
       capability "Switch"

       command "heaterOn"
       command "heaterOff"
       command "nextHeaterMode"
       command "setPointUp"
       command "setPointDown"

       attribute "currentHeaterMode", "String"
       attribute "supportedHeaterModes", "String"

       attribute "airTemp", "Number"
       attribute "waterTemp", "Number"
       attribute "setPoint", "Number"

       if (isHE) {
           command "setHeaterMode", [[name:"Heater mode*",
                                      "type":"ENUM",
                                      "description":"Heater mode to set (from the supported heater modes list)",
                                      "constraints":["Off", "Heater", "Solar Pref", "Solar Only"]]]

           command "setHeaterSetpoint", [[name:"Heater Setpoint*",
                                      "type":"ENUM",
                                      "description":"Set the heater set point",
                                      "constraints":[50,51,52,53,54,55,56,57,58,59,
                                                     60,61,62,63,64,65,66,67,68,69,
                                                     70,71,72,73,74,75,76,77,78,79,
                                                     80,81,82,82,84,85,86,87,88,89,
                                                     90,91,92,93,94,95,96,97,98,99,
                                                     100,101,102,103,104]
                                     ]]
       } else {
           // ST version of commands goes here

       }
    }

	preferences {
         section("General:") {
            input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "None",
        	    "Error",
        	    "Warning",
        	    "Info",
        	    "Debug",
        	    "Trace"
        	],
        	defaultValue: "Info",
            displayDuringSetup: true,
        	required: false
            )
        }
    }
}

def installed() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Info'
    getHubPlatform()
}

def updated() {
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Info'
}

def refresh() {
    logger("Requested a refresh","info")
    def body = null
    sendGet("/state/temps", 'parseTemps', body, data)
}

def parseTemps(response, data=null) {
    def temps = response.json
    logger("parseTemps - ${temps}","debug")
    if (temps.units) {
        state.units = "°" + temps.units.name
    }

    if (temps.air) {
        sendEvent([name: "airTemp", value: temps.air.toInteger(), unit: state.units])
    }

    parseBodies(response, data)
}

def parseBodies(response, data=null) {
    def bodies = response.json.bodies
    logger("parseBodies - ${bodies}","debug")
    if (bodies) {
        bodies.each {
            logger("Current body - ${it} id=${it.id}-","debug")
            logger("${it.id.toInteger()} ===?== ${getDataValue('bodyID').toInteger()} --- ${it.id.toInteger() == getDataValue('bodyID').toInteger()}","trace")
            if (it.id.toInteger() == getDataValue('bodyID').toInteger()) {
                parse(it)
                sendGet("/config/body/${it.id}/heatModes", 'parseHeatModes', body, data)
                return
            }
        }
    }
}

def parse(body) {
    logger("Parse body - ${body}","trace")
    sendEvent([name: "setPoint", value: body.setPoint, unit:  state.units])
    if (body.heatMode instanceof java.lang.Integer) {
        sendEvent([name: "currentHeaterMode", value: getHeatMode(body.heatMode)])
    } else {
        sendEvent([name: "currentHeaterMode", value: body.heatMode.desc])
    }
    if (body.containsKey('isOn')) { sendEvent([name: "switch", value: body.isOn ? "on" : "off" ]) }
    if (body.containsKey('temp')) { sendEvent([name: "waterTemp", value: body.temp.toInteger(), unit: state.units]) }
}

def parseHeatModes(response, data=null) {
    def heatModes = response.json
    logger("Parse modes - ${heatModes}","trace")
    if (heatModes) {
        def modes = new ArrayList<String>()
        heatModes.each {
            modes.add(it.desc)
        }
        logger("Heater Modes ${modes}", "debug")

        sendEvent([name: "supportedHeaterModes", value: modes, unit: null])
    }
}

def getHeatMode(intModeValue) {
    switch (intModeValue) {
        case 0:
           return "Off"
           break;
        case 1:
           return "Heater"
           break;
        case 2:
           return "Solar Pref"
           break;
        case 4:
           return "Solar"
           break;
        default:
           return "Unknown"
           logger("Unknown Heater mode - ${intModeValue}","error")
           break;
    }
}

def getHeatModeID(mode) {
    switch(mode) {
         case "Off":
            return 0
            break;
         case "Heater":
            return 1
            break;
         case "Solar Pref":
            return 2
            break;
         case "Solar":
            return 4
            break;
         default:
            return 0
            logger("Unknown Heater mode - ${mode}","error")
            break;
      }
}

def nextHeaterMode() {
	logger("Going to nextMode()", "debug")

    def currentMode = device.currentValue("currentHeaterMode")
    logger("Current Heater Mode index ${currentMode}", "debug")

	def heatModesStr = device.currentValue("supportedHeaterModes")
    def heatModes = heatModesStr[1..heatModesStr.length() - 2].tokenize(",")

    int index = heatModes.indexOf(currentMode)
    logger("Current Heater Mode index ${index}", "debug")
    if (index >= heatModes.size() - 1) {
        index = -1;
    }
    index = index + 1
    logger("Next Heater Mode index ${index}", "debug")

    setHeaterMode(heatModes.get(index))
}

def getChildDNI(name) {
	return getParent().getChildDNI(getDataValue("bodyID") + "-" + name)
}

def on() {
    int id = getDataValue("circuitID")
    def body = "{\"id\": ${id}, \"state\": 1}"
    logger("Turn on circuit ${id}","debug")
    sendPut("/state/circuit/setState", 'stateChangeCallback', body, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def off() {
    int id = getDataValue("circuitID")
    def body = "{\"id\": ${id}, \"state\": 0}"
    logger("Turn off curcuit ${id}","debug")
    sendPut("/state/circuit/setState", 'stateChangeCallback', body, data  )
    sendEvent(name: "switch", value: "off", displayed:false,isStateChange:false)
}

def stateChangeCallback(response, data) {
    logger("State Change Response ${response.getStatus() == 200 ? 'Success' : 'Failed'}","info")
    logger("State Change Response ${response.getStatus()}","debug")
}

// **********************************
// Heater control functions to update the current heater state / setpoints on the poolController.
// spdevice is the child device with the correct DNI to use in referecing SPA or POOL
// **********************************
def heaterOn(spDevice) {
  setHeaterMode("Heater")
}

def heaterOff(spDevice) {
  setHeaterMode("Off")
}

def setHeaterMode(mode) {
    def id = getDataValue("bodyID")
    def body = [id: id.toInteger(), heatMode: getHeatModeID(mode)]
    logger("Set Body heatMode with ${body}","debug")
    sendPut("/state/body/heatMode", 'setModeCallback', body, data )
    sendEvent(name: "currentHeaterMode", value: mode)
}

def setModeCallback(response, data=null) {
    logger("Set Mode Response ${response.getStatus()}","trace")
}

def setHeaterSetpoint(setPoint) {    
    def id = getDataValue("bodyID")
    logger("GOT ID ${id}","debug")
    def body = [id : id.toInteger(), setPoint: setPoint ]
    logger("Set Body setPoint with ${body}","debug")
    sendPut("/state/body/setPoint", 'setPointCallback', body, data )
    sendEvent(name: "setPoint", value: setPoint, , unit:  state.units)
}

def setPointCallback(response, data=null) {
    logger("Set Heating Setpoint Response ${response.getStatus()}","trace")
}

def setPointUp() {
    def sp = device.currentValue("setPoint")
    def newSP = sp.toInteger() + 1
    logger("SetPoint up from ${sp} to ${newSP}","debug")
    setHeaterSetpoint(newSP)
}

def setPointDown() {
    def sp = device.currentValue("setPoint")
    def newSP = sp.toInteger() - 1
    logger("SetPoint down from ${sp} to ${newSP}","debug")
    setHeaterSetpoint(newSP)
}

// **********************************
// INTERNAL Methods
// **********************************
def addHESTChildDevice(namespace, deviceType, dni, options  ) {
	if (state.isHE) {
    	return addChildDevice(namespace, deviceType, dni, options)
	} else {
    	return addChildDevice(namespace, deviceType, dni, location.hubs[0]?.id, options)
    }
}

private getHost() {
    return getParent().getHost()
}

def getControllerURI(){
    String host = getHost()
    return "http://${host}"
}

private sendGet(message, aCallback=generalCallback, body="", data=null) {
    def params = [
        uri: getControllerURI(),
        path: message,
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Send GET to with ${params} CB=${aCallback}","debug")
    if (state.isST) {
    	 def hubAction = physicalgraph.device.HubAction.newInstance(
               [
                method: "GET",
                path: message,
                body: body,
                headers: [
                    HOST: getHost(),
                    "Accept":"application/json"
                    ]
               ],
               null,
               [
                callback : aCallback,
                type: 'LAN_TYPE_CLIENT'
               ])
        sendHubCommand(hubAction)
    } else {
        asynchttpGet(aCallback, params, data)
    }
}

private sendPut(message, aCallback=generalCallback, body="", data=null) {
    logger("Send PUT to ${message} with ${body} and ${aCallback}","debug")
    if (state.isST) {
        def hubAction = physicalgraph.device.HubAction.newInstance(
               [
                method: "PUT",
                path: message,
                body: body,
                headers: [
                    HOST: getHost(),
                    "Accept":"application/json"
                    ]
               ],
               null,
               [
                callback : aCallback,
                type: 'LAN_TYPE_CLIENT'
               ])
        sendHubCommand(hubAction)
    } else {
     	def params = [
        	uri: getControllerURI(),
        	path: message,
        	requestContentType: "application/json",
        	contentType: "application/json",
        	body:body
    	]
        asynchttpPut(aCallback, params, data)
    }
}

def generalCallback(response, data) {
   logger("Callback(status):${response.getStatus()}","debug")
}

def toIntOrNull(it) {
   return it?.isInteger() ? it.toInteger() : null
}

//*******************************************************
//*  logger()
//*
//*  Wrapper function for all logging.
//*******************************************************
private logger(msg, level = "debug") {

    def lookup = [
        	    "None" : 0,
        	    "Error" : 1,
        	    "Warning" : 2,
        	    "Info" : 3,
        	    "Debug" : 4,
        	    "Trace" : 5]
     def logLevel = lookup[state.loggingLevelIDE ? state.loggingLevelIDE : 'Debug']
     // log.debug("Lookup is now ${logLevel} for ${state.loggingLevelIDE}")

    switch(level) {
        case "error":
            if (logLevel >= 1) log.error msg
            break

        case "warn":
            if (logLevel >= 2) log.warn msg
            break

        case "info":
            if (logLevel >= 3) log.info msg
            break

        case "debug":
            if (logLevel >= 4) log.debug msg
            break

        case "trace":
            if (logLevel >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

// **************************************************************************************************************************
// SmartThings/Hubitat Portability Library (SHPL)
// Copyright (c) 2019, Barry A. Burke (storageanarchy@gmail.com)
//
// The following 3 calls are safe to use anywhere within a Device Handler or Application
//  - these can be called (e.g., if (getPlatform() == 'SmartThings'), or referenced (i.e., if (platform == 'Hubitat') )
//  - performance of the non-native platform is horrendous, so it is best to use these only in the metadata{} section of a
//    Device Handler or Application
//
private String  getPlatform() { (physicalgraph?.device?.HubAction ? 'SmartThings' : 'Hubitat') }	// if (platform == 'SmartThings') ...
private Boolean getIsST()     { (physicalgraph?.device?.HubAction ? true : false) }					// if (isST) ...
private Boolean getIsHE()     { (hubitat?.device?.HubAction ? true : false) }						// if (isHE) ...
//
// The following 3 calls are ONLY for use within the Device Handler or Application runtime
//  - they will throw an error at compile time if used within metadata, usually complaining that "state" is not defined
//  - getHubPlatform() ***MUST*** be called from the installed() method, then use "state.hubPlatform" elsewhere
//  - "if (state.isST)" is more efficient than "if (isSTHub)"
//
private String getHubPlatform() {
    if (state?.hubPlatform == null) {
        state.hubPlatform = getPlatform()						// if (hubPlatform == 'Hubitat') ... or if (state.hubPlatform == 'SmartThings')...
        state.isST = state.hubPlatform.startsWith('S')			// if (state.isST) ...
        state.isHE = state.hubPlatform.startsWith('H')			// if (state.isHE) ...
    }
    return state.hubPlatform
}
private Boolean getIsSTHub() { (state.isST) }					// if (isSTHub) ...
private Boolean getIsHEHub() { (state.isHE) }
