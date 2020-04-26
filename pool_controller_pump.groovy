/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pump
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pool Controller Pump",
        namespace: "bsileo",
        author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_pump.groovy') {
	capability "Switch"
    capability "PowerMeter"
    capability "Refresh"
    attribute "RPM", "number"
    attribute "Status", "string"
    attribute "friendlyName", "string"
    attribute "circuitFunction", "string"
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

def installed() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def updated () {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def refresh() {
    def id = getDataValue("pumpID")
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/pump/${id})"
    ]
    logger("Sending refresh with ${params}","debug")
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh(response, data) {
    logger("parserefresh - ${response} - ${data}","trace")
    def json = response.getJson()
    logger("parserefresh - ${res}","trace")
    sendEvent(name: "power", value: json.watts)
    sendEvent(name: "RPM", value: json.rpm)
    sendEvent(name: "flow", value: json.flow)
    sendEvent(name: "driveState", value: json.driveState)
    sendEvent(name: "command", value: json.command)
    sendEvent(name: "mode", value: json.mode)
    
}

def on() {    
    def id = getDataValue("circuitID")
    def data = [id: id, state: 1]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/circuit/setState"
    ]
    logger("Turn on pump with ${params} - ${data}","debug")
    asynchttpPut('stateChangeCallback', params, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def off() {	
    def id = getDataValue("circuitID")
    def data = [id: id, state: 0]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/circuit/setState)"
    ]
    logger("Turn off pump with ${params}","debug")
    asynchttpPut('stateChangeCallback', params, data)
    sendEvent(name: "switch", value: "off", displayed:false,isStateChange:false)
}

def stateChangeCallback(response, data) {
    logger("State Change Result ${response.getStatus()}","debug")   
    logger("State Change Result Data ${response.getData()}","debug")   
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
