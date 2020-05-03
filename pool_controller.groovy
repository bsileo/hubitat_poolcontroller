/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller - Main Device
 *
 *  Author: Brad Sileo
 *
 */

metadata {
	definition (name: "Pool Controller", namespace: "bsileo", author: "Brad Sileo") {
        capability "Refresh"
        capability "Configuration"
        attribute "LastUpdated", "String"
        attribute "Freeze", "Boolean"
        attribute "Mode", "String"
        attribute "ConfigControllerLastUpdated", "String"

        // Not working....disable for now
        /*command "updateAllLogging",  [[name:"Update All Logging",
                                       type: "ENUM",
                                       description: "Pick a logging settings for me and all child devices",
                                       constraints: [
        	                                "0" : "None",
        	                                "1" : "Error",
        	                                "2" : "Warning",
        	                                "3" : "Info",
        	                                "4" : "Debug",
        	                                "5" : "Trace"
        	                            ]
                                      ] ]*/
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
  refreshConfiguration(true)
}

def installed() {
    refreshConfiguration(true)
}

def updated() {
  refreshConfiguration(true)
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def manageChildren() {
	logger( "Pool Controller manageChildren starting","debug")
    manageTempSensors()
    manageBodies()
    managePumps()
    manageHeaters()
    manageCircuits()
    manageFeatureCircuits()
    manageChlorinators()
    manageIntellichem()
    manageLightGroups()
    refresh()
}

def manageTempSensors() {
    def airTemp = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
    if (!airTemp) {
        logger(("Create Air temp child device"),"debug")
        airTemp = addChildDevice("hubitat","Generic Component Temperature Sensor", getChildDNI("airTemp"),
                                 [ label: "${device.displayName} Air Temperature", componentName: "airTemp", componentLabel: "${device.displayName} Air Temperature",
                                  isComponent:false, completedSetup:true])
    }


    def solarTemp = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})
    if (!solarTemp) {
    	logger(("Create Solar temp child device"),"debug")
       	solarTemp = addChildDevice("hubitat","Generic Component Temperature Sensor", getChildDNI("solarTemp"),
                                  [ label: "${device.displayName} Solar Temperature", componentName: "solarTemp", componentLabel: "${device.displayName} Solar Temperature",
                                   isComponent:false, completedSetup:true])
    }
}

def manageBodies() {
    def bodies = state.bodies
    bodies.forEach { value ->
        if (value.isActive) {
            def body = getChild("body",value.id)
            if (!body) {
                logger(("Create BODY child device"),"debug")
                body = addChildDevice("bsileo","Pool Controller Body", getChildDNI("body",value.id),
                    [
                        label: "${device.displayName} ${value.name}",
                        componentName: "body ${value.id}",
                        componentLabel: "${device.displayName} ${value.name} Body",
                        bodyID: value.id,
                        circuitID: value.circuit.toString(),
                        isComponent:false,
                        completedSetup:true
                    ]
                )
            } else {
                body.updateDataValue("circuitID",value.circuit.toString())
            }
        }
    }
}

def managePumps () {
    def pumps = state.pumps
    pumps.forEach { value ->
        if (value.isActive) {
            def pName = "Pump ${value.id}"
            if (value.name != null) { pName = value.name }
            def pump = getChild("pump",value.id)
            if (!pump) {
                def cID = value.circuits ? value.circuits[0].circuit : ''
                pump = addChildDevice("bsileo","Pool Controller Pump", getChildDNI("pump",value.id),
                                 [completedSetup: true,
                                    label: "${device.displayName} (${pName})",
                                    componentLabel:"${device.displayName} (${pName})",
                                    isComponent:false,
                                    componentName: pName,
                                    pumpID: value.id.toString(),
                                    pumpType: value.type.toString(),
                                    circuitID: cID.toString()
                                 ])
                logger( "Created Pump Child ${pName}","info")
            } else {
                pump.updateDataValue("pumpType",value.type.toString())
                pump.updateDataValue("circuitID",cID.toString())
                logger( "Updated Pump Child ${pName}","info")
            }
        }
    }
}

def manageHeaters() {
    def heaters = state.heaters
    heaters.forEach {data ->
        if (data.isActive) {
            def heat = getChild("heater",data.id)
            def label = "${device.displayName} ${data.name}"
            if (!heat) {
                def name = "heater${data.id}"
                heat = addChildDevice("bsileo","Pool Controller Heater", getChildDNI("heater",data.id),
                                  [completedSetup: true,
                                   label: label ,
                                   isComponent:false,
                                   componentName: name,
                                   bodyID: data.body,
                                   circuitID: data.body,
                                   componentLabel:label
                                 ])
                logger( "Created Pool Heat ${label}" ,"info")
            } else {
                heat.updateDataValue("bodyID", data.body.toString())
                heat.updateDataValue("circuitID", data.body.toString())
                logger( "Updated Pool Heat ${label}" ,"debug")
            }
        }
    }
}

def manageFeatureCircuits() {
    def circuits = state.features
    circuits.forEach {data ->
        if (data.isActive) {
            def auxname = "feature${data.id}"
            try {
                def auxButton = getChild("feature",data.id)
                if (!auxButton) {
                    def auxLabel = "${device.displayName} Feature ${data.name}"
                	log.info "Create Feature switch ${auxLabel} Named=${auxname}"
                    auxButton = addChildDevice("hubitat","Generic Component Switch", getChildDNI("feature",data.id),
                            [
                                completedSetup: true,
                                label: auxLabel,
                                isComponent:false,
                                componentName: auxname,
                                componentLabel: auxLabel,
                                typeID: data.type.toString(),
                                circuitID: data.id.toString()
                             ])
                    logger( "Success - Created Feature switch ${auxname}" ,"debug")
                }
                else {
                    auxButton.updateDataValue("typeID",data.type.toString())
                    auxButton.updateDataValue("circuitID",data.id.toString())
                    logger("Found existing Feature Switch ${auxname} and Updated it","info")
                }
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
                logger( "Failed to create Feature Switch ${auxname}" + e ,"error")
            }
        }
    }
}

def manageCircuits() {
    def circuits = state.circuits
    circuits.forEach {data ->
        if (data.friendlyName == "NOT USED") return
        if (data.isActive) {
            def auxname = "circuit${data.id}"
            def auxLabel = "${device.displayName} Circuit ${data.name}"
            try {
                def auxButton = getChild("circuit",data.id)
                if (!auxButton) {
                	log.info "Create Circuit switch ${auxLabel} Named=${auxname}"
                    auxButton = addChildDevice("hubitat","Generic Component Switch", getChildDNI("circuit",data.id),
                            [
                                completedSetup: true,
                                label: auxLabel,
                                isComponent:false,
                                componentName: auxname,
                                componentLabel: auxLabel,
                                typeID: data.type.toString(),
                                circuitID: data.id.toString()
                             ])
                    logger( "Success - Created switch ${auxname}" ,"debug")
                }
                else {
                    auxButton.updateDataValue("typeID",data.type.toString())
                    auxButton.updateDataValue("circuitID",data.id.toString())
                    logger("Found existing Circuit ${auxname} Updated","info")
                }
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
                logger( "Failed to create Pool Controller Switch ${auxname}" + e ,"error")
            }
        }
    }
}


def manageChlorinators() {
    def chlors = state.chlorinators
    logger("chlors->${chlors}","trace")
    if (!chlors) {
       logger("No Chlorinator devices found","info")
       return
   }
    chlors.each {data ->
        if (data.isActive) {
            def name = "chlorinator-${data.id}"
            def label = "${device.displayName} Chlorinator ${data.id}"
            def chlor = getChild("chlorinator",data.id)
            if (!chlor) {
                chlor = addChildDevice("bsileo","Pool Controller Chlorinator", getChildDNI("chlorinator",data.id),
                                  [completedSetup: true,
                                   label: label ,
                                   isComponent:false,
                                   componentName: name,
                                   id: data.id,
                                   address: data.address.toString(),
                                   componentLabel:label
                                 ])
                logger( "Created Pool Chlorinator ${label}" ,"info")
            } else {
                chlor.updateDataValue("address", data.address.toString())
                logger( "Updated Pool Chlorinator ${label}" ,"info")
            }
        }
    }
}

def manageIntellichem() {
   def chems = state.intellichem
   if (!chems) {
       logger("No Intellichem devices found","info")
       return
   }
   chems.each {data ->
        if (data.isActive) {
            def name = "intellichem${data.id}"
            def label = "${device.displayName} Intellichem ${data.id}"
            try {
                def existing = getChild("intellichem",data.id)
                if (!existing) {
                	log.info "Create Intellichem ${auxLabel} Named=${name}"
                    existing = addChildDevice("bsileo","Pool Controller Intellichecm", getChildDNI("intellichem",data.id),
                            [
                                completedSetup: true,
                                label: auxLabel,
                                isComponent:false,
                                componentName: name,
                                componentLabel: label
                             ])
                    logger( "Success - Created ${name}" ,"debug")
                }
                else {
                    logger("Found existing ${name} Updated","info")
                }
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
                logger( "Failed to create Intellichem ${name}" + e ,"error")
            }
        }
    }
}

def manageLightGroups() {
	logger( "Create/Update Light Children for this device","debug")
    def light = state.intellibrite
    if (light) {
        if (light.isActive) {
            try {
                def cID = light.circuits ? light.circuits[0].circuit : ''
                def existing = getChild("intellibrite",light.id)
                if (!existing) {
                	def name = "intellibrite${light.id}"
                    logger("Creating Intellibrite Named ${name}","trace")
                    def label = "${device.displayName} Intellibrite ${light.id}"
                    existing = addChildDevice("bsileo","Pool Controller Intellibrite", getChildDNI("intellibrite",light.id),
                            [
                                completedSetup: true,
                                label:label,
                                isComponent:false,
                                componentName: name,
                                componentLabel: label,
                                circuitID: cID
                             ])
                    logger( "Created Intellibrite ${name}" ,"info")
                }
                else {
                    existing.updateDataValue("circuitID",cID.toString())
                    logger("Found existing Intellibrite ${light.id} Updated","info")
                }
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
                logger( "Failed to create Intellibrite ${name}-" + e ,"error")
            }
        }
    } else {
       logger( "No Intellibrites present","info")
    }
}


// ******************************************************************
// Update my configuration from the controller into my STATE
// If process is true, we also manageChildren() after the state is update
// ******************************************************************
def refreshConfiguration(process = false) {
    if (process) {
        sendGet("/config",'configurationCallback')
    } else {
        sendGet("/config",'parseConfiguration')
    }
}


def configurationCallback(response, data) {
    if (parseConfiguration(response, data)) {
        manageChildren()
    } else {
        logger("Failed to process configuration ${response}","error")
    }
}

def parseConfiguration(response, data) {
    def msg = response.json
    logger(msg,"trace")
    state.bodies = msg.bodies
    state.circuits = msg.circuits
    state.features = msg.features
    state.pumps = msg.pumps
    state.valves = msg.valves
    state.heaters = msg.heaters
    state.chlorinators = msg.chlorinators
    state.intellibrite = msg.intellibrite
    state.configLastUpdated = msg.lastUpdated
    logger(state,"trace")
    return true
}

def updateAllLogging(level) {
    levels = ["None" : "0",
        	   "Error" : "1",
        	   "Warning" : "2",
        	   "Info" : "3",
        	   "Debug" : "4",
        	   "Trace": "5"]
    levelID = levels[level]
    logger("LEVEL=${level}->${levelID}","error")
    device.updateSetting("configLoggingLevelIDE", level)
    state.loggingLevelIDE = levelID
    childDevices.each {
        try {
            it.updateSetting("configLoggingLevelIDE", level)
            // it.updateSetting("configLoggingLevelIDE", levelID)
        }
        catch (e) {
            logger("Error setting Logging on ${it} - ${e}","trace")
        }
    }
    logger("Logging set to level ${level}(${settings.loggingLevelIDE})","info")
}

def refresh() {
    sendGet("/state/temps", parseTemps)
    sendGet("/config/all", parseConfig)
    childDevices.each {
        try {
            it.refresh()
        }
        catch (e) {
            logger("No refresh method on ${child} - ${e}","trace")
        }
    }
}


def parseConfig(response, data) {
    if (response.getStatus() == 200) {
        def json = response.getJson()
        def date = new Date()
        sendEvent([[name:"LastUpdated", value:"${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}", descriptionText:"Last updated at ${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}"]])
        def lastUpdated = json.lastUpdated
        sendEvent([[name:"ConfigControllerLastUpdated", value:lastUpdated, descriptionText:"Last updated time is ${lastUpdated}"]])
	}
}

def parseTemps(response, data) {
    logger("Parse Temps ${response.getStatus()} -- ${response.getStatus()==200}","debug")
    if (response.getStatus() == 200) {
        logger("Process ${response.getJson()}","trace")
        def at = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
        def solar = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})
        String unit = "°${location.temperatureScale}"
        response.getJson().each {k, v ->
            logger("Process ${k} ${v}","trace")
           switch (k) {
        	 case "air":
                at?.parse([[name:"temperature", value:v, descriptionText:"${at?.displayName} temperature is ${v}${unit}", unit: unit]])
            	break
             case "solar":
                solar?.parse([[name:"temperature", value:v, descriptionText:"${solar?.displayName} temperature is ${v}${unit}", unit: unit]])
            	break
            default:
            	break
          }
        }
	}
}
// **********************************************
// inbound PARSE
// **********************************************
def parse(raw) {
    logger( "Parsing ${raw}","trace")
    def msg = parseLanMessage(raw)
    logger( "Parsing ${msg}","trace")
    logger( "Full msg: ${msg}","trace")
    logger( "HEADERS: ${msg.headers}","trace")
    def type = msg.headers['X-EVENT-TYPE']
    logger("Parse event of type: ${type}","info")
    logger( "JSON: ${msg.json}","debug")
    Date date = new Date()
    sendEvent([[name:"LastUpdated", value:"${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}", descriptionText:"Last updated at ${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}"]])
    if (msg.json) {
        switch(type) {
            case "temps":
                if (msg.json.bodies) {parseDevices(msg.json.bodies, 'body')}
                break
            case "circuit":
                parseCircuit(msg.json)
                break
            case "feature":
                parseFeature(msg.json)
                break
            case "body":
                parseDevice(msg.json, 'body')
                break
            case "controller":
                parseController(msg.json)
                break
            case "virtualCircuit":
                break
            case "config":
                parseConfig(msg.json)
                break
            case "pump":
                parseDevice(msg.json, 'pump')
                break
            case "chlorinator":
                parseDevice(msg.json, 'chlorinator')
                break
            default:
                logger( "No handler for incoming event type '${type}'","warn")
                break
       }
    }
}


def parseDevices(msg, type) {
    logger("Parsing ${type} - ${msg}","debug")
    msg.each { section ->
       parseDevice(section, type)
    }
}

def parseDevice(section,type) {
    logger("Parse Device of ${type} from ${section}","debug")
    logger("Device is ${getChild(type, section.id)}","trace")
    getChild(type, section.id)?.parse(section)
}

def parseCircuit(msg) {
    logger("Parsing circuit - ${msg}","debug")
    def child = getChild("circuit",msg.id)
    logger("Parsing circuit ${child}")
    if (child) {
        def val = msg.isOn ? "on": "off"
        child.parse([[name:"switch",value: val, descriptionText: "Status changed from controller to ${val}" ]])
    }
}

def parseConfig(msg) {
    // No processing on config messages - these contain invalid data versus the current state so just let them go, use Configure to update
    //parseDevices(msg.bodies, 'body')
    //parseDevices(msg.pumps, 'pump')
    //parseDevices(msg.chlorinators, 'chlorinator')
    //parseDevices(msg.intellichem, 'intellichem')
}

def parseController(msg) {
    logger("Parsing controller - ${msg}","debug")

}

def parseFeature(msg) {
    logger("Parsing feature - ${msg}","debug")
    def child = getChild("feature",msg.id)
    logger("Parsing feature ${child}","trace")
    if (child) {
        def val = msg.isOn ? "on": "off"
        child.parse([[name:"switch",value: val, descriptionText: "Status changed from controller to ${val}" ]])
    }
}

def getChild(type,id) {
    def dni = getChildDNI(type,id)
    logger("Find child with ${type}-${id}","trace")
    return getChildDevices().find { element ->
        return element.deviceNetworkId == dni
      }
}

def getChildCircuit(id) {
	// get the circuit device given the ID number only (e.g. 1,2,3,4,5,6)
    // also check for features as it could be one of them!
    def child = getChild("circuit",id)
    if (!child) {
        child = getChild("feature",id)
    }
}

def getChildDNI(name) {
	return getDataValue("controllerMac") + "-" + name
}

def getChildDNI(type, name) {
    return getDataValue("controllerMac") + "-${type}-${name}"
}


// **********************************
// PUMP Control
// **********************************

def poolPumpOn() {
	return setCircuit(poolPumpCircuitID(),1)
}

def poolPumpOff() {
	return setCircuit(poolPumpCircuitID(),0)
}

def spaPumpOn() {
	logger( "SpaPump ON","debug")
	return setCircuit(spaPumpCircuitID(),1)
}

def spaPumpOff() {
	return setCircuit(spaPumpCircuitID(),0)
}

// **********************************
// Component Interfaces
// **********************************
def componentRefresh(device) {
    logger("Got REFRESH Request from ${device}","trace")

}

def componentOn(device) {
	logger("Got ON Request from ${device}","debug")
	return setCircuit(device,1)
}

def componentOff(device) {
	logger( "Got OFF from ${device}","debug")
	return setCircuit(device,0)
}

def childCircuitID(device) {
	logger("CCID---${device}","trace")
	return toIntOrNull(device.getDataValue("circuitID"))
}

def setCircuit(device, state) {
  def id = childCircuitID(device)
  logger( "Executing setCircuit with ${device} - ${id} to ${state}","debug")
  sendPut("/state/circuit/setState", setCircuitCallback, [id: id, state: state], [id: id, newState: state, device: device])
}

def setCircuitCallback(response, data) {
    if (response.getStatus() == 200) {
        logger("Circuit update Succeeded","info")
        logger("SetCircuitCallback(data):${data}","debug")
        def dev = data.device
        def newState = data.newState.toString() == "1" ? 'on' : 'off'
        logger("SetCircuitCallback-Sending:${newState} to ${dev}","debug")
        dev.sendEvent([name:'switch', value: newState , textDescription: "Set to ${newState}"])
    } else {
        logger("Ciurcuit update failed with code ${response.getStatus()}","error")
    }
}

// **********************************
// INTERNAL Methods
// **********************************
def getHost() {
  def ip = getDataValue('controllerIP')
  def port = getDataValue('controllerPort')
  return "${ip}:${port}"
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
     def params = [
        uri: getControllerURI(),
        path: message,
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Send PUT to ${message} with ${params} and ${aCallback}","debug")
    asynchttpPut(aCallback, params, data)
}

private sendDelete(message, aCallback=generalCallback, body="") {
    def params = [
        uri: getControllerURI(),
        path: message,
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    logger("Send GET to with ${params}","debug")
    asynchttpDelete(aCallback, params, data)
}

def generalCallback(response, data) {
   logger("Callback(status):${response.getStatus()}","debug")
}


private updateDeviceNetworkID(){
  setDeviceNetworkId()
}


private setDeviceNetworkId(){
  	def hex = getDataValue('controllerMac').toUpperCase().replaceAll(':', '')
    if (device.deviceNetworkId != "$hex") {
        device.deviceNetworkId = "$hex"
        logger( "Device Network Id set to ${device.deviceNetworkId}","debug")
    }
}

private String convertHostnameToIPAddress(hostname) {
    def params = [
        uri: "http://dns.google.com/resolve?name=" + hostname,
        contentType: 'application/json'
    ]

    def retVal = null

    try {
        retVal = httpGet(params) { response ->
            log.trace "Request was successful, data=$response.data, status=$response.status"
            //log.trace "Result Status : ${response.data?.Status}"
            if (response.data?.Status == 0) { // Success
                for (answer in response.data?.Answer) { // Loop through results looking for the first IP address returned otherwise it's redirects
                    //log.trace "Processing response: ${answer}"
                    if (isIPAddress(answer?.data)) {
                        log.trace "Hostname ${answer?.name} has IP Address ${answer?.data}"
                        return answer?.data // We're done here (if there are more ignore it, we'll use the first IP address returned)
                    } else {
                        log.trace "Hostname ${answer?.name} redirected to ${answer?.data}"
                    }
                }
            } else {
                log.warn "DNS unable to resolve hostname ${response.data?.Question[0]?.name}, Error: ${response.data?.Comment}"
            }
        }
    } catch (Exception e) {
        log.warn("Unable to convert hostname to IP Address, Error: $e")
    }

    //log.trace "Returning IP $retVal for Hostname $hostname"
    return retVal
}


// gets the address of the Hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}


private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

// TEMPERATUE Functions
// Get stored temperature from currentState in current local scale

def getTempInLocalScale(state) {
	def temp = device.currentState(state)
	def scaledTemp = convertTemperatureIfNeeded(temp.value.toBigDecimal(), temp.unit).toDouble()
	return (getTemperatureScale() == "F" ? scaledTemp.round(0).toInteger() : roundC(scaledTemp))
}

// Get/Convert temperature to current local scale
def getTempInLocalScale(temp, scale) {
	def scaledTemp = convertTemperatureIfNeeded(temp.toBigDecimal(), scale).toDouble()
	return (getTemperatureScale() == "F" ? scaledTemp.round(0).toInteger() : roundC(scaledTemp))
}

// Get stored temperature from currentState in device scale
def getTempInDeviceScale(state) {
	def temp = device.currentState(state)
	if (temp && temp.value && temp.unit) {
		return getTempInDeviceScale(temp.value.toBigDecimal(), temp.unit)
	}
	return 0
}

def getTempInDeviceScale(temp, scale) {
	if (temp && scale) {
		//API return/expects temperature values in F
		return ("F" == scale) ? temp : celsiusToFahrenheit(temp).toDouble().round(0).toInteger()
	}
	return 0
}

def roundC (tempC) {
	return (Math.round(tempC.toDouble() * 2))/2
}

 def toIntOrNull(it) {
   return it?.isInteger() ? it.toInteger() : null
 }

def sync(ip, port) {
	def existingIp = getDataValue("controllerIP")
	def existingPort = getDataValue("controllerPort")
	if (ip && ip != existingIp) {
		updateDataValue("ControllerIP", ip)
	}
	if (port && port != existingPort) {
		updateDataValue("controllerPort", port)
	}
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