/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Intellichem
 *
 *  Author: Brad Sileo
 *
 *
 *  version: 1.2
 */
metadata {
	definition (name: "Pool Controller Intellichem", namespace: "bsileo", author: "Brad Sileo" )
        {
		capability "Refresh"
        capability "pHMeasurement"

		attribute "ORPlevel", "number"
        attribute "ORPsetPoint", "number"
		attribute "ORPdosingStatusDesc", "string"
		attribute "ORPdosingStatusName", "string"
        attribute "ORPtankLevel", "number"
        attribute "ORPtankCapacity", "number"

        attribute "pHlevel", "number"
        attribute "pHsetPoint", "number"
        attribute "pHtemperature", "number"
        attribute "pHtankLevel", "number"
        attribute "pHtankCapacity", "number"
		attribute "pHPdosingStatusDesc", "string"
		attribute "pHdosingStatusName", "string"

		attribute "lastComm", "string"
        attribute "body", "string"
        attribute "status", "string"

        attribute "alkalinity", "number"
        attribute "calciumHardness", "number"
        attribute "cyanuricAcid", "number"

		command "refresh"

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
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Debug'
}

def updated() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Debug'
}

def initialize() {
}

def refresh() {
    logger("Requested a refresh","info")
	def body = null
    def data = null
    def id = getDataValue('chemID').toInteger()
    logger("Refresh Intellichem #${id}","debug")
    // sendGet("/config/intellichem",'parseRefresh', body, data)
    sendGet("/state/chemController/${id}",'parseRefresh', body, data)
}

def parseRefresh (response, data=null) {
     if (response.getStatus() == 200) {
        def json = response.getJson()
        return parse(json)
     } else {
         logger("Failed to refresh from server - ${response.getStatus()}","error")
         logger("Error data is ${response.getErrorMessage()}","error")
     }
}

// Process the results from /state/chemController/{id}
def parse(section) {
    section.each { key, v ->
        switch (key) {
            case "ph":
                parsePh(v)
                break;
            case "ORP":
                parseORP(v)
                break;
            case "alarms":
                parseAlarms(v)
                break;
            case "warnings":
                parseWarnings(v)
                break;
            case "status":
                sendEvent(name: "status", value: v.name)
                break;
            case "lastComm":
                sendEvent(name: "lastComm", value: v)
                break;
            case "alkalinity":
                sendEvent(name: k, value: v)
                break;
            case "calciumHardness":
                sendEvent(name: k, value: v)
                break;
            case "cyanuricAcid":
                sendEvent(name: k, value: v)
                break;
            default:
                //logger( "No handler for incoming Intellichem data element '${key}'","trace")
                break
        }
    }
}

def parsePh(section) {
    sendEvent(name: 'pHTemperature',
                value: section.probe.temperature,
                unit: section.probe.tempUnits.name)
    sendEvent(name: 'pH', value: section.probe.level)
    sendEvent(name: 'pHTemperature', value: section.probe.temperature)
    sendEvent(name: 'pHtankLevel', value: section.tenk.level)
    sendEvent(name: 'pHtankCapacity', value: section.tenk.capacity)

}

def parseORP(section) {
    sendEvent(name: 'ORPlevel', value: section.probe.level)
    sendEvent(name: 'ORPsetPoint', value: section.setPoint)
    sendEvent(name: 'ORPtankLevel', value: section.tank.level)
    sendEvent(name: 'ORPtankCapacity', value: section.tank.capacity)
    sendEvent(name: 'ORPdosingStatusName', value: section.dosingStatus.name)
    sendEvent(name: 'ORPdosingStatusDesc', value: section.dosingStatus.desc)
}

def parseAlarms(section) {
    section.each {k,v ->
        if (v.val == 1) {
            sendEvent(name: 'alarm',
            value: k,
            descriptionText: "Alarm of type ${k} - ${v.desc}")
        }
    }
}

def parseWarnings(section) {
    section.each {k,v ->
        if (v.val == 1) {
            sendEvent(name: 'warning',
            value: k,
            descriptionText: "Warning of type ${k} - ${v.desc}")
        }
    }
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
