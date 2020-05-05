/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller - Body
 *
 *  Author: Brad Sileo
 *
 */

metadata {
	definition (name: "Pool Controller Body", namespace: "bsileo", author: "Brad Sileo",
                importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_body.groovy') {
       capability "Refresh"
       capability "Configuration"
       capability "Switch"
       capability "TemperatureMeasurement"
       attribute "setPoint", "Number"
       attribute "heatMode", "String"
       if (isHT) {
           command "setHeaterMode", [[name:"Heater mode*",
                                      "type":"ENUM",
                                      "description":"Heater mode to set",
                                      "constraints":["Off", "Heater", ,"Solar Pref","Solar Only"]]]

           command "setHeaterSetPoint", [[name:"Heater SetPoint*",
                                      "type":"ENUM",
                                      "description":"Set the heater set point",
                                      "constraints":[50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,82,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104]
                                     ]]
        }

        command "heaterOn"
       command "heaterOff"
    }

	preferences {
         section("General:") {
            input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "0" : "None",
        	    "1" : "Error",
        	    "2" : "Warning",
        	    "3" : "Info",
        	    "4" : "Debug",
        	    "5" : "Trace"
        	],
        	defaultValue: "3",
            displayDuringSetup: true,
        	required: false
            )
        }
    }
}

def configure() {
  logger( "Executing 'configure()'","info")
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def installed() {
	manageChildren()
    getHubPlatform()
}

def updated() {
  manageChildren()
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def manageChildren() {
	logger( "Pool Controller Body manage Children...","debug")
}


def parse(body) {
    logger("Parse body - ${body}","trace")
    sendEvent([name: "setPoint", value: body.setPoint])
    if (body.heatMode instanceof java.lang.Integer) {
        sendEvent([name: "heatMode", value: body.heatMode == 1 ? "Heater" : "Off"])
    } else {
        sendEvent([name: "heatMode", value: body.heatMode.desc])
    }
    String unit = "°${location.temperatureScale}"
    if (body.containsKey('isOn')) { sendEvent([name: "switch", value: body.isOn ? "On" : "Off" ]) }
    if (body.containsKey('temp')) { sendEvent([name: "temperature", value: body.temp.toInteger(), unit: unit]) }
}

def refresh() {
    logger("Requested a refresh","info")
    def body = null
    def params = [
        uri: getParent().getControllerURI(),
        path: "/config/options/bodies",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.get('parseRefresh', params, data)
    } else {
        asynchttpGet('parseRefresh', params, data)
    }

    params.path = "/state/temps"
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.get('parseRefresh', params, data)
    } else {
        asynchttpGet('parseRefresh', params, data)
    }
}

def parseRefresh (response, data) {
    logger("body.parseRefresh - ${response.json}","debug")
    def bodies = response.json.bodies
    if (bodies) {
        parseBodies(bodies)
    }
}

def parseBodies(bodies) {
    logger("parseBodies - ${bodies}","debug")
    bodies.each {
        // logger("${it.id.toInteger()} ===?== ${getDataValue('bodyID').toInteger()} --- ${it.id.toInteger() == getDataValue('bodyID').toInteger()}","trace")
        if (it.id.toInteger() == getDataValue('bodyID').toInteger()) {
            parse(it)
        }
    }
}

def getHeatMode(intModeValue) {
    if (intModeValue == 1) { return "Heat" }
    else if (intModeValue == 0) { return "Off" }
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

def getChildDNI(name) {
	return getParent().getChildDNI(getDataValue("bodyID") + "-" + name)
}


def on() {
    def id = getDataValue("circuitID")
    def body = [id: id, state: 1]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/circuit/setState",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Turn on body with ${params} - ${body}","debug")
     if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.put('stateChangeCallback', params, body)
    } else {
        asynchttpPut('stateChangeCallback', params, body)
    }
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def off() {
    def id = getDataValue("circuitID")
    def body = [id: id, state: 0]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/circuit/setState",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Turn off body with ${params}","debug")
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.put('stateChangeCallback', params, body)
    } else {
        asynchttpPut('stateChangeCallback', params, body)
    }
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
   def body = [id: id, mode: getHeatModeID(mode)]
   def params = [
        uri: getParent().getControllerURI(),
        path: "/state/body/heatMode",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Set Body heatMode with ${params} and ${body}","debug")
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.put('setModeCallback', params, body)
    } else {
        asynchttpPut('setModeCallback', params, body)
    }

    sendEvent(name: "heatMode", value: mode)
}

def setModeCallback(response, data) {
    logger("Set Mode Response ${response.getStatus()}","trace")
    logger("Set Mode Data ${data}","trace")
}


def setHeaterSetPoint(setPoint) {
    def id = getDataValue("bodyID")
    def body = [id: id, setPoint: setPoint]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/body/setPoint",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Set Body setPoint with ${params} to ${data}","debug")
    if (state.isST) {
    	include 'asynchttp_v1'
    	asynchttp_v1.put('setPointCallback', params, data)
    } else {
        asynchttpPut('setPointCallback', params, data)
    }
    sendEvent(name: "setPoint", value: setPoint)
}

def setPointCallback(response, data) {
    logger("State Change Response ${response.getStatus()}","trace")
    logger("State Change Data ${data}","trace")
}



// INTERNAL Methods
private getHost() {
    return getParent().getHost()
}




/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/

private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
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