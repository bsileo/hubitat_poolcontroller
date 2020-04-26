/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Chlorinator
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pool Controller Chlorinator",
            namespace: "bsileo",
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_chlorinator.groovy') {
		capability "Refresh"
        capability "Switch"
		attribute "saltPPM", "string"
		attribute "currentOutput", "string"
		attribute "superChlorinate", "string"
		attribute "status", "string"
		attribute "poolSpaSetpoint", "string"
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
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def installed() {
}

def updated() {
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}


def initialize() {

}

// Command Implementations
def refresh() {
    logger("Requested a refresh","info")
    def id = getDataValue("id")
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/${id}"
    ]
    logger("Refresh with ${params} - ${data}","debug")
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh (response, data) {
    logger("parseRefresh - ${response.json}","debug")
    try {
        def value = response.getJson()
        sendEvent([name: "currentOutput", value: value.currentOutput])
        sendEvent([name: "targetOutput", value: value.targetOutput])
        sendEvent([name: "saltLevel", value: value.saltLevel])
        sendEvent([name: "saltRequired", value: value.saltRequired ? 'Yes' : 'No'])
        if (value.status) {
            sendEvent([name: "status-${value.status.name}", value: value.status.val])
        }
    }
    catch (e) {
        logger("Failed to refresh Chlors due to ${e}","error")
    }
}


def on() {
    return chlorinatorOn()
}

def off() {
   return chlorinatorOff()
}

def chlorinatorOn() {
   return chlorinatorUpdate(70,30,0)
}

def chlorinatorOff() {
   return chlorinatorUpdate(0,0,0)
}

def chlorinatorUpdate(poolLevel, spaLevel, superChlorHours) {
    def id = getDataValue("id")
    def data = [
        id: id,
        poolSetPoint: poolLevel,
        spaSetPoint: spaLevel,
        superChlorHours: superChlorHours
    ]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/setChlor)"
    ]
    logger("Update Chlorinator with ${data}","info")
    logger("Update Chlorinator with PUT ${params} - ${data}","debug")
    asynchttpPut('updateCallback', params, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def updateCallback(response, data) {
    logger("State Change Result ${response.getStatus()}","debug")
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
