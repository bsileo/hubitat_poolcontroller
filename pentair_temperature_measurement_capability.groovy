/**
 *  Copyright 2019 Brad Sileo
 *
 *
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pentair Temperature Measurement Capability", 
			namespace: "bsileo", 
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_temperature_measurement_capability.groovy') {
		capability "Temperature Measurement"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	try {
         def pair = description.split(":")
    	 return createEvent(name: pair[0].trim(), value: pair[1].trim(), unit:"F")
     }
     catch (java.lang.ArrayIndexOutOfBoundsException e) {
           log.debug "Error! " + e + "-Parsing:'" + description + "'"
    }    
}

def setTemperature(t) {
	log.debug(device.label + " set to ${t}") 
    sendEvent(name: 'temperature', value: t, unit:"F")
}