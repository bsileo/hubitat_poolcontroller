/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Intellichem
 *
 *  Author: Brad Sileo
 *
 *
 *  version: 1.1
 */
metadata {
	definition (name: "Pool Controller Intellichem", namespace: "bsileo", author: "Brad Sileo" )
        {
		capability "Refresh"
        capability "pHMeasurement"

		attribute "ORP", "string"
        attribute "waterFlow", "string"
		attribute "salt", "string"
		attribute "tank1Level", "string"
		attribute "tank2Level", "string"
        attribute "status1", "string"
        attribute "status2", "string"
        attribute "CYA", "string"
		attribute "CH", "string"
        attribute "TA", "string"
        attribute "SI", "string"

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
    logger("Refresh Intellichem with ${params} - ${data}","debug")
    sendGet("/config/intellichem",'parseRefresh', body, data)
    sendGet("/state/intellichem",'parseRefresh', body, data)
}

def parse(section) {
    section.each { key, v ->
        switch (key) {
            case "ph":
                sendEvent(name: "pH", value: v)
                break;
            case "ORP":
                sendEvent(name: "ORP", value: v)
                break;
            case "waterFlow":
                val = v ? "NO FLOW": "Flow OK"
                sendEvent(name: "flowAlarm", value: val)
                break;
            case "salt":
                sendEvent(name: "salt", value: v)
                break;
            case "tank1Level":
                sendEvent(name: "tank1Level", value: v)
                break;
            case "tank2Level":
                sendEvent(name: "tank2Level", value: v)
                break;
            case "status1":
                sendEvent(name: "status1", value: v)
                break;
            case "status2":
                sendEvent(name: "status2", value: v)
                break;
            // Start of "STATE" items
            case "CYA":
                sendEvent(name: "CYA", value: v)
                break;
            case "CH":
                sendEvent(name: "CH", value: v)
                break;
            case "TA":
                sendEvent(name: "TA", value: v)
                break;
            case "SI":
                sendEvent(name: "SI", value: v)
                break;
            default:
                logger( "No handler for incoming Intellichem data element '${key}'","warn")
                break
        }
    }
}

def parseRefresh (response, data=null) {
     if (response.getStatus() == 200) {
        def json = response.getJson()
        return refresh(json)
     } else {
         logger("Failed to refresh from server - ${response.getStatus()}","error")
         logger("Error data is ${response.getErrorMessage()}","error")
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
