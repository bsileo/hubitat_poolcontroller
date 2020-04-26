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
		attribute "flowAlarm", "string"
		attribute "SI", "string"
		attribute "setpointpH", "string"
		attribute "setpointORP", "string"
		attribute "CYA", "string"
		attribute "CALCIUMHARDNESS", "string"
		attribute "TOTALALKALINITY", "string"
		attribute "tankpH", "string"
		attribute "tankORP", "string"
		attribute "modepH", "string"
		attribute "modeORP", "string"

		command "refresh"

	}
}

def installed() {

}

def updated() {
}

def initialize() {
}

def parse(json)
{
	def group;
    def subjson;
    def name;
    def val;
	json.each { g, s ->
         group = g;
         subjson = s;
         switch (group) {
        	case "readings":
            	subjson.each { k, v ->
                	switch (k) {
                    	case "PH":
                            sendEvent(name: "pH", value: v)
                          break;
                        case "ORP":
                            sendEvent(name: "ORP", value: v)
                          break;
                        case "WATERFLOW":
                        	val = v ? "NO FLOW": "Flow OK"
                            sendEvent(name: "flowAlarm", value: val)
                          break;
                        case "SI":
                            sendEvent(name: "SI", value: v)
                          break;
                    }
                }
              break;
            case "settings":
            	subjson.each { k, v ->
                	switch (k) {
                    	case "PH":
                            sendEvent(name: "setpointpH", value: (v*10))
                          break;
                        case "ORP":
                            sendEvent(name: "setpointORP", value: (v/10))
                          break;
                        case "CYA":
                            sendEvent(name: "CYA", value: v)
                          break;
                        case "CALCIUMHARDNESS":
                            sendEvent(name: "CALCIUMHARDNESS", value: v)
                          break;
                        case "TOTALALKALINITY":
                            sendEvent(name: "TOTALALKALINITY", value: v)
                          break;
                    }
                }
              break;
            case "tankLevels":
                subjson.each { k, v ->
                	switch (k) {
                    	case "1":
                			sendEvent(name: "tankpH", value: v)
              	  	      break;
                    	case "2":
                			sendEvent(name: "tankORP", value: v)
              	  	      break;
                     }
                 }
              break;
            case "mode":
                subjson.each { k, v ->
                	switch (k) {
                    	case "1":
                          switch (v) {
                            case "85":
                              sendEvent(name: "modepH", value: "Mixing")
                            break;
                            case "21":
                              sendEvent(name: "modepH", value: "Dosing")
                            break;
                            case "101":
                              sendEvent(name: "modepH", value: "Monitoring")
                            break;
                            default:
                              sendEvent(name: "modepH", value: v)
                            break;
                            }
              	  	      break;
                    	case "2":
                          switch (v) {
							case "32":
                              sendEvent(name: "modeORP", value: "Mixing")
                            break;
                            case "34":
                              sendEvent(name: "modeORP", value: "Dosing")
                            break;
                            default:
                              sendEvent(name: "modeORP", value: v)
                            break;
                          }
              	  	      break;
                    }
                }
              break;
         }
    }
}


// Command Implementations
def poll() {
	refresh()
}

def refresh() {
    pollDevice()
}

def pollDevice() {
    parent.poll()
}

// set the local value for the heatingSetpoint. Does NOT update the parent / Pentair platform!!!
def setORPSetpoint(v) {
	def timeNow = now()
    if (v) {
    	if (!state.heatingSetpointTriggeredAt || (1 * 2 * 1000 < (timeNow - state.ORPSetpointTriggeredAt))) {
			state.ORPSetpointTriggeredAt = timeNow
			sendEvent(name: "ORPSetpoint", value:(state.ORPSetpoint*10), eventType: "ENTITY_UPDATE", displayed: true)
            // parent.setORPSetpoint(state.ORPSetpoint*10)
		}
	}
}

// set the local value for the heatingSetpoint. Doesd NOT update the parent / Pentair platform!!!
def setpHSetpoint(v) {
	log.debug "setpHSetpoint " + v
	state.pHSetpointTriggeredAt = timeNow
	sendEvent(name: "pHSetpoint", value:(state.pHSetpoint/10), eventType: "ENTITY_UPDATE", displayed: true)
    // parent.setpHSetpoint(state.pHSetpoint/10)
}

def setTankLevelpH(level) {
	sendEvent("name": "tankpH", "value": level, eventType: "ENTITY_UPDATE", displayed: true)
    // parent.updateTankpH(device,level)
}

def setTankLevelORP(level) {
	sendEvent("name": "tankORP", "value": level, eventType: "ENTITY_UPDATE", displayed: true)
    //parent.updateTankORP(device,level)
}

