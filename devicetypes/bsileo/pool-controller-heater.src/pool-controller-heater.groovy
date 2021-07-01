/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Heater
 *
 *  Author: Brad Sileo
 *
 *
 *  version: 1.1
 */

metadata {
	definition (name: "Pool Controller Heater",  namespace: "bsileo", author: "Brad Sileo" )
        {
		capability "Temperature Measurement"
		capability "Refresh"
        attribute "heatingSetpoint", "NUMBER"
        attribute "heaterMode",  "string"
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
}

def updated() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Info'
}

def refresh() {
    logger("Requested a refresh","info")
    sendGet("/state/temps",'parseRefresh', body, data)
}

def parseRefresh (response, data=null) {
    def json = response.getJson()
    logger("parseRefresh - ${json}","debug")
    def bodies = json.bodies
    def units = json.units
    if (units) {
        state.units = "°" + units.name
    }
    if (bodies) {
        parseBodies(bodies)
    }
}

def parseBodies(bodies) {
    logger("parseBodies - ${bodies}","debug")
    bodies.each {
        if (it.circuit.toInteger() == getDataValue('bodyID').toInteger()) {
            sendEvent([name: "heatingSetPoint", value: it.setPoint, unit: state.units])
            sendEvent([name: "heaterMode", value: it.heatMode.name])
            sendEvent([name: "temperature", value: it.temp, unit: state.units])
        }
    }
}


def setTemperature(t) {
    logger("Current temp setting to ${t} ${state.units}"."debug")
    sendEvent(name: 'temperature', value: t, unit:state.units)
    log.debug("DONE current temp set to ${t}","trace")
}


// **********************************
// INTERNAL Methods
// **********************************
def addHESTChildDevice(namespace, deviceType, dni, options  ) {
  	return addChildDevice(namespace, deviceType, dni, options)
}

// INTERNAL Methods
private getHost() {
    return getParent().getHost()
}

def getControllerURI(){
    def host = getHost()
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
    asynchttpGet(aCallback, params, data)
}

private sendPut(message, aCallback=generalCallback, body="", data=null) {
    logger("Send PUT to ${message} with ${params} and ${aCallback}","debug")
    def params = [
        uri: getControllerURI(),
        path: message,
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    asynchttpPut(aCallback, params, data)
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