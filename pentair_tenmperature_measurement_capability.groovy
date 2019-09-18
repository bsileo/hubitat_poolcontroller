/**
 *  Copyright 2019 Brad Sileo
 *
 *
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pentair Temperature Measurement Capability", namespace: "bsileo", 
                   author: "Brad Sileo") {
		capability "Temperature Measurement"
	}

	// simulator metadata
	simulator {
		for (int i = 0; i <= 100; i += 10) {
			status "${i} F": "temperature:$i"
		}
	}

	// UI tile definitions
	tiles {
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}°', unit:"F",
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
			)
		}
		main "temperature"
		details "temperature"
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