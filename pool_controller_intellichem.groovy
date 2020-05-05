/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Intellichem
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pool Controller Intellichem",
            namespace: "bsileo",
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_intellichem.groovy')
    {
		capability "Refresh"
        capability "pHMeasurement"

		attribute "pH", "string"
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
    getHubPlatform()
}

def updated() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def initialize() {
}

def refresh() {
    logger("Requested a refresh","info")
     def params = [
        uri: getParent().getControllerURI(),
        path: "/config/intellichem"
    ]
    logger("Refresh Intellichem with ${params} - ${data}","debug")
    asynchttpGet('parseRefresh', params, data)
    params.path = "/state/intellichem"
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh (response, data) {
     if (response.getStatus() == 200) {
        def json = response.getJson()
	    json.each { key, v ->
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
             }
        }
     } else {
         logger("Failed to refresh from server - ${response.getStatus()}","error")
         logger("Error data is ${response.getErrorMessage()}","error")
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
