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
