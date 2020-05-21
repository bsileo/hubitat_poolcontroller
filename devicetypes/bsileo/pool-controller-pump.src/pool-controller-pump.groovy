/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pump
 *
 *  Author: Brad Sileo
 *
 *
 *  version: 0.9.1
 */
metadata {
	definition (name: "Pool Controller Pump",
        namespace: "bsileo",
        author: "Brad Sileo"
        {
	    capability "Switch"
        capability "PowerMeter"
        capability "Refresh"
        attribute "RPM", "number"
        attribute "Status", "String"
        attribute "flow", "number"
        attribute "driveState", "number"
        attribute "mode", "number"
        attribute "command", "String"
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
    
    if (isST) {
        tiles (scale:2) { 
         	valueTile("switch", "switch", decoration: "flat", width: 2, height: 2) {
                state "default", label:'${currentValue}'
            }
            valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
                state "power", label:'${currentValue} Watts'
            }
            valueTile("RPM", "RPM", decoration: "flat", width: 2, height: 2) {
                state "default", label:'${currentValue} RPM'
            }
            valueTile("status", "Status", decoration: "flat", width: 2, height: 2) {
                state "default", label:'Status: ${currentValue}'
            }
            valueTile("flow", "flow", decoration: "flat", width: 2, height: 2) {
                state "default", label:'Flow: ${currentValue}'
            }
            valueTile("driveState", "driveState", decoration: "flat", width: 2, height: 2) {
                state "default", label:'Drive State: ${currentValue}'
            }
            valueTile("mode", "mode", decoration: "flat", width: 2, height: 2) {
                state "default", label:'Mode: ${currentValue}'
            }
            valueTile("command", "command", decoration: "flat", width: 2, height: 2) {
                state "default", label:'Command: ${currentValue}'
            }
            standardTile("refresh", "refresh", width:1, height:1, inactiveLabel: false, decoration: "flat") {
				state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
			}
        }
        main "switch"
    }
    
    
}

def installed() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 'Info'
    getHubPlatform()
}

def updated () {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 'Info'
}


def parse(json) {
    logger("Parse Pump - ${json}","debug")
    sendEvent([name: "power", value: json.watts])
    sendEvent([name: "RPM", value: json.rpm])
    sendEvent([name: "flow", value: json.flow])
    sendEvent([name: "driveState", value: json.driveState])
    sendEvent([name: "command", value: json.command])
    sendEvent([name: "mode", value: json.mode])
    sendEvent([name: "switch", value: json.rpm > 0 ? 'on' : 'off'])
    if (json.status) {
        sendEvent([name: "Status", value: json.status.name, descriptionText: "Pump status is currently ${json.status.desc}"])
    }

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
    logger("parseRefresh - ${res}","trace")
    parse(json)
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