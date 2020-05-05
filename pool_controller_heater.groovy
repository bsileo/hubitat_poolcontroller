/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Heater
 *
 *  Author: Brad Sileo
 *
 */

metadata {
	definition (name: "Pool Controller Heater",
            namespace: "bsileo",
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_water_thermostat.groovy') {
		capability "Temperature Measurement"
		capability "Refresh"
        attribute "heatingSetpoint", "NUMBER"
        attribute "heaterMode",  "string"
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
    initialize()
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
    getHubPlatform()
}

def updated() {
    initialize()
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def initialize() {
    state.scale = "F"
}

def refresh() {
    logger("Requested a refresh","info")
     def params = [
        uri: getParent().getControllerURI(),
        path: "/state/temps"
    ]
    logger("Refresh Heater with ${params} - ${data}","debug")
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh (response, data) {
    def json = response.getJson()
    logger("parseRefresh - ${json}","debug")
    def bodies = json.bodies
    def unit = json.units
    if (bodies) {
        parseBodies(bodies)
    }
    if (units) {
        state.scale = units.name
    }
}

def parseBodies(bodies) {
    logger("parseBodies - ${bodies}","debug")
    bodies.each {
        if (it.circuit.toInteger() == getDataValue('bodyID').toInteger()) {
            sendEvent([name: "heatingSetPoint", value: it.setPoint])
            sendEvent([name: "heaterMode", value: it.heatMode.name])
            sendEvent([name: "temperature", value: it.temp])
        }
    }
}


def setTemperature(t) {
	log.debug(device.label + " current temp set to ${t}")
    sendEvent(name: 'temperature', value: t, unit:"F")
    log.debug(device.label + " DONE current temp set to ${t}")
}

// Get stored temperature from currentState in current local scale
def getTempInLocalScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInLocalScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

// get/convert temperature to current local scale
def getTempInLocalScale(temp, scale) {
	if (temp && scale) {
		def scaledTemp = convertTemperatureIfNeeded(temp.toBigDecimal(), scale).toDouble()
		return (getTemperatureScale() == "F" ? scaledTemp.round(0).toInteger() : roundC(scaledTemp))
	}
	return 0
}

def getTempInDeviceScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInDeviceScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

def getTempInDeviceScale(temp, scale) {
	if (temp && scale) {
		def deviceScale = (state.scale == 1) ? "F" : "C"
		return (deviceScale == scale) ? temp :
				(deviceScale == "F" ? celsiusToFahrenheit(temp).toDouble().round(0).toInteger() : roundC(fahrenheitToCelsius(temp)))
	}
	return 0
}

def roundC (tempC) {
	return (Math.round(tempC.toDouble() * 2))/2
}

/**
 *
 *
 *  Standard code for API calls
 **/
private getHost() {
    return getParent().getHost()
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