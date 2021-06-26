/**
 *  Copyright 2021 Brad Sileo
 *
 *  Pool Controller - Main Device representing the nJSPC
 *
 *  Author: Brad Sileo
 *
 *  Version: "1.11"
 *
 */

metadata {
	definition (name: "Pool Controller", namespace: "bsileo", author: "Brad Sileo") {

        capability "Refresh"
        capability "Configuration"
        capability "Actuator"

        attribute "LastUpdated", "String"
        attribute "Freeze", "Boolean"
        attribute "Mode", "String"
        attribute "ConfigControllerLastUpdated", "String"
		attribute "waterSensor1", "Number"
        attribute "Pool-temperature", "Number"
        attribute "Pool-heatStatus", "String"
        attribute "Spa-temperature", "Number"
        attribute "Spa-heatStatus", "String"

    }

	preferences {
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
        	required: false
        )
    }
}

def configure() {
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Debug'
  refreshConfiguration(true)
}

def installed() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Debug'
    refreshConfiguration(true)
}

def updated() {
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Debug'
  refreshConfiguration(true)
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
    def namespace = 'hubitat'
    def deviceType = 'Generic Component Temperature Sensor'
    def airTemp = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
    if (!airTemp) {
        	airTemp = addHESTChildDevice(namespace,deviceType, getChildDNI("airTemp"),
            	                     [ label: getChildName("Air Temperature"),
                                      componentName: "airTemp",
                                      componentLabel: getChildName("Air Temperature"),
                	                  isComponent:true,
                                      completedSetup:true])
	    logger("Created Air temperature child device","info")
    }

    def solarTemp = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})
    if (!solarTemp) {
       		solarTemp = addHESTChildDevice(namespace,deviceType, getChildDNI("solarTemp"),
                                  [ label: getChildName("Solar Temperature"),
                                  componentName: "solarTemp",
                                  componentLabel: getChildName("Solar Temperature"),
                                   isComponent:true,
                                   completedSetup:true])
        logger(("Created Solar temperature child device"),"info")
    }
}

def manageBodies() {
    def bodies = state.bodies
    logger("Process bodies ${bodies}","trace")
    bodies.each { value ->
        if (value.isActive) {
            def body = getChild("body",value.id)
            if (!body) {
                logger(("Create BODY child device"),"debug")
                	body = addHESTChildDevice("bsileo","Pool Controller Body", getChildDNI("body",value.id),
                    [
                        label: getChildName(value.name),
                        componentName: "body ${value.id}",
                        componentLabel: getChildName(value.name),
                        bodyID: value.id.toString(),
                        circuitID: value.circuit.toString(),
                        isComponent:false
                    ]
                )
                logger( "Created new Body called ${value.name}","info")
            } else {
                body.updateDataValue("circuitID",value.circuit.toString())
                body.updateDataValue("bodyID",value.id.toString())
                logger( "Found existing Body called ${value.name} and updated it","info")
            }
        }
    }
}

def managePumps () {
    def pumps = state.pumps
    pumps.each { value ->
        if (value.isActive) {
            def pName = "Pump ${value.id}"
            if (value.name != null) { pName = value.name }
            def pump = getChild("pump",value.id)
            def cID = value.circuits ? value.circuits[0].circuit : ''
            if (!pump) {
                pump = addHESTChildDevice("bsileo","Pool Controller Pump", getChildDNI("pump",value.id),
                            [completedSetup: true,
                                label: getChildName(pName),
                                componentLabel:getChildName(pName),
                                isComponent:false,
                                componentName: pName,
                                pumpID: value.id.toString(),
                                pumpType: value.type.toString(),
                                circuitID: cID.toString()
                            ])
                logger( "Created new Pump called ${pName}","info")
            } else {
                pump.updateDataValue("pumpType",value.type.toString())
                pump.updateDataValue("circuitID",cID.toString())
                logger( "Found existing Pump called ${pName} and updated it","info")
            }
        }
    }
}

def manageHeaters() {
    def heaters = state.heaters
    heaters.each {data ->
        if (data.isActive) {
            def heat = getChild("heater",data.id)
            def label = getChildName(data.name)
            if (!heat) {
                def name = "heater${data.id}"
                heat = addHESTChildDevice("bsileo","Pool Controller Heater", getChildDNI("heater",data.id),
                                 [completedSetup: true,
                                   label: label ,
                                   isComponent:false,
                                   componentName: name,
                                   bodyID: data.body,
                                   circuitID: data.body,
                                   heaterID: data.id,
                                   componentLabel:label
                                 ])
                logger( "Created new Heater called ${label}" ,"info")
            } else {
                heat.updateDataValue("heaterID", data.id.toString())
                heat.updateDataValue("bodyID", data.body.toString())
                heat.updateDataValue("circuitID", data.body.toString())
                logger( "Found existing Heater called ${label} and updated it" ,"info")
            }
        }
    }
}

def manageFeatureCircuits() {
    def circuits = state.features
    def namespace = 'hubitat'
    def deviceType = 'Generic Component Switch'

    circuits.each {data ->
        if (data.isActive) {
            def auxname = "feature${data.id}"
            try {
                def auxButton = getChild("feature",data.id)
                if (!auxButton) {
                    def auxLabel = getChildName(data.name)
                	log.info "Create Feature switch ${auxLabel} Named=${auxname}"
                    auxButton = addHESTChildDevice(namespace,deviceType, getChildDNI("feature",data.id),
                            [
                                completedSetup: true,
                                label: auxLabel,
                                isComponent:false,
                                componentName: auxname,
                                componentLabel: auxLabel,
                                typeID: data.type.toString(),
                                circuitID: data.id.toString()
                             ])
                    logger( "Success - Created Feature switch ${data.name}" ,"debug")
                }
                else {
                    auxButton.updateDataValue("typeID",data.type.toString())
                    auxButton.updateDataValue("circuitID",data.id.toString())
                    logger("Found existing Feature Switch for ${data.name} and updated it","info")
                }
            }
            catch(e)
            {
                logger( "Failed to create Feature Switch for ${data.name}" + e ,"error")
            }
        }
    }
}

def manageCircuits() {
  	def namespace = 'hubitat'
    def deviceType = "Generic Component Switch"
    def circuits = state.circuits
    circuits.each {data ->
        if (data.friendlyName == "NOT USED") return
        if (data.isActive) {
            def auxname = "circuit${data.id}"
            def auxLabel = getChildName(data.name)
            try {
                def auxButton = getChild("circuit",data.id)
                if (!auxButton) {
                	log.info "Create Circuit switch ${auxLabel} Named=${auxname}"
                    auxButton = addHESTChildDevice(namespace,deviceType, getChildDNI("circuit",data.id),
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
                    logger("Found existing Circuit for ${data.name} and updated it","info")
                }
            }
            catch(e)
            {
                logger( "Failed to create Pool Controller Circuit for ${data.name}" + e ,"error")
            }
        }
    }
}


def manageChlorinators() {
    def chlors = state.chlorinators
    logger("chlors->${chlors}","trace")
    if (!chlors) {
       logger("No Chlorinator devices found on Controller","info")
       return
   }
    chlors.each {data ->
        if (data.isActive) {
            def name = "chlorinator-${data.id}"
            def label = getChildName("Chlorinator ${data.id}")
            def chlor = getChild("chlorinator",data.id)
            if (!chlor) {
                	chlor = addHESTChildDevice("bsileo","Pool Controller Chlorinator", getChildDNI("chlorinator",data.id),
                                  [completedSetup: true,
                                   label: label ,
                                   isComponent:false,
                                   componentName: name,
                                   chlorId: data.id,
                                   address: data.address.toString(),
                                   componentLabel:label
                                 ])
                logger( "Created Pool Chlorinator ${label}" ,"info")
            } else {
                chlor.updateDataValue("address", data.address.toString())
                chlor.updateDataValue("chlorId", data.id.toString())
                logger( "Found existing Pool Chlorinator ${label} and updated it" ,"info")
            }
        }
    }
}

def manageIntellichem() {
   def chems = state.chemControllers
   if (!chems) {
       logger("No Intellichem devices found on Controller","info")
       return
   }
   chems.each {data ->
        if (data.isActive) {
            def name = "intellichem${data.id}"
            def label = getChildName("Intellichem ${data.id}")
            try {
                def existing = getChild("intellichem",data.id)
                if (!existing) {
                	log.info "Create Intellichem ${auxLabel} Named=${name}"
                    existing = addHESTChildDevice("bsileo","Pool Controller Intellichem", getChildDNI("intellichem",data.id),
                            [
                                completedSetup: true,
                                label: label,
                                isComponent:false,
                                componentName: name,
                                componentLabel: label
                             ])
                    logger( "Success - Created ${name}" ,"debug")
                }
                else {
                    logger("Found existing Intellichem ${name} and updated it","info")
                }
            }
            catch(e)
            {
                logger( "Failed to create Intellichem ${name}" + e ,"error")
            }
        }
    }
}

def manageLightGroups() {
	logger( "Create/Update Light Children for this device","debug")
    def lights = state.lightGroups
     if (!lights) {
       logger("No Light Groups found on Controller","info")
       return
   }
   lights.each {light ->
        if (light.isActive) {
            try {
                def cID = light.circuits ? light.circuits[0].circuit : ''
                def existing = getChild("lightGroup",light.id)
                if (!existing) {
                	def name = "lightGroup${light.id}"
                    logger("Creating Light Group Named ${name}","trace")
                    def label = getChildName(light.name)
                    existing = addHESTChildDevice("bsileo","Pool Controller LightGroup", getChildDNI("lightGroup",light.id),
                            [
                                completedSetup: true,
                                label:label,
                                isComponent:false,
                                componentName: name,
                                componentLabel: label,
                                circuitID: cID,
                                lightGroupID: light.id
                             ])
                    logger( "Created Light Group ${name}" ,"info")
                }
                else {
                    existing.updateDataValue("circuitID",cID.toString())
                    existing.updateDataValue("lightGroupID",light.id.toString())
                    logger("Found existing Light Group ${light.id} and updated it","info")
                }
            }
            catch(e)
            {
                logger( "Failed to create Light Group ${name}-" + e ,"error")
            }
        }
   }
}


def getChildName(name) {
    def result = name
    def prefix = getDataValue("prefixNames") == "true" ? true : false
    if (prefix) {
        result = "${device.displayName} (${name})"
    }
    return result
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

def configurationCallback(response, data=null) {
    if (parseConfiguration(response, data)) {
        manageChildren()
    } else {
        logger("Failed to process configuration ${response}","error")
    }
}

def parseConfiguration(response, data=null) {
    def msg = response.json
    logger(msg,"trace")
    state.bodies = msg.bodies
    state.circuits = msg.circuits
    state.features = msg.features
    state.pumps = msg.pumps
    state.valves = msg.valves
    state.heaters = msg.heaters
    state.chlorinators = msg.chlorinators
    state.chemControllers = msg.chemControllers
    state.intellibrite = msg.intellibrite
    state.configLastUpdated = msg.lastUpdated
    state.lightGroups = msg.lightGroups
    state.remote = msg.remotes
    state.eggTimers = msg.eggTimers
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
    sendGet("/config/all", parseConfigAll)
    childDevices.each {
        try {
            it.refresh()
        }
        catch (e) {
            logger("No refresh method on ${child} - ${e}","trace")
        }
    }
}


def parseConfigAll(response, data=null) {
    if (response.getStatus() == 200) {
        def json = response.getJson()
        def date = new Date()
        sendEvent([[name:"LastUpdated", value:"${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}", descriptionText:"Last updated at ${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}"]])
        def lastUpdated = json.lastUpdated
        sendEvent([[name:"ConfigControllerLastUpdated", value:lastUpdated, descriptionText:"Last updated time is ${lastUpdated}"]])
	}
}

def parseTemps(response, data=null) {
    logger("Parse Temps ${response.getStatus()} -- ${response.getStatus()==200}","debug")
    if (response.getStatus() == 200) {
        parseTempsResult(response.getJson())
    }
}


def parseTempsResult(json) {
    def at = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
    def solar = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})
    String unit = "°${location.temperatureScale}"
    if (json.units) {
        unit = "°" + json.units.name
    }
    json.each {k, v ->
        logger("Process Temps Elements ${k} ${v}","trace")
        switch (k) {
            case "air":
                at?.parse([[name:"temperature", value:v, descriptionText:"${at?.displayName} value is ${v}${unit}", unit: unit]])
                break
            case "solar":
                solar?.parse([[name:"temperature", value:v, descriptionText:"${solar?.displayName} value is ${v}${unit}", unit: unit]])
                break
            case "waterSensor1":
                sendEvent([[name:"waterSensor1", value: v, descriptionText:"Update temperature of Water Sensor 1 to ${v}"]])
                break
            case "waterSensor2":
                sendEvent([[name:"waterSensor2", value: v, descriptionText:"Update temperature of Water Sensor 2 to ${v}"]])
                break
            case "waterSensor3":
                sendEvent([[name:"waterSensor3", value: v, descriptionText:"Update temperature of Water Sensor 3 to ${v}"]])
                break
            case "waterSensor4":
                sendEvent([[name:"waterSensor4", value: v, descriptionText:"Update temperature of Water Sensor 4 to ${v}"]])
                break
            case "bodies":
                logger("Got bodies","trace")
                parseTempsBodies(v)
                break
            case "units":
                // NoOp - handled above
                break
            default:
                logger("Unhandled Temperature Element '${k}' : ${v}","debug")
                break
        }
    }
}

def parseTempsBodies(bodies) {
	logger("Parse Temps Bodies ${bodies}","trace")
	bodies.each {body ->
    	sendEvent([[name:"${body.name}-temperature", value: body.temp, descriptionText:"Temperature of Body ${body.name} is ${body.temp}"]])
        sendEvent([[name:"${body.name}-heatStatus", value: body.heatStatus.name, descriptionText:"Heater of Body ${body.name} is ${body.heatStatus.desc}"]])
    }
}

// **********************************************
// inbound PARSE
// **********************************************
def parse(raw) {
    logger( "Parsing Raw = ${raw}","trace")
    def msg = parseLanMessage(raw)
    logger( "Parse msg: ${msg}","debug")
    logger( "HEADERS: ${msg.headers}","trace")
    def type = msg.headers['X-EVENT-TYPE']
    logger("Parse event of type: ${type}","info")
    logger( "Parse JSON payload: ${msg.json}","debug")
    Date date = new Date()
    sendEvent([[name:"LastUpdated", value:"${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}", descriptionText:"Last updated at ${date.format('MM/dd/yyyy')} ${date.format('HH:mm:ss')}"]])
    if (msg.json) {
        switch(type) {
            case "temps":
                parseTempsResult(msg.json)
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
            case "lightGroup":
                parseDevice(msg.json, 'lightGroup')
                break
            case "chemController":
                parseDevice(msg.json, 'intellichem')
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
        logger("Set val to ${val} for ${child}","debug")
        child.parse([[name:"switch",value: val, descriptionText: "Status changed from controller to ${val}" ]])
    }
    logger("parseFeature Done","debug")
}

def getChild(systemID) {
    logger("Find child with ${systemID}","trace")
    return getChildDevices().find { element ->
        return element.id == systemID
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

def setCircuitCallback(response, data=null) {
    if (response.getStatus() == 200) {
        logger("Circuit update Succeeded","info")
        logger("SetCircuitCallback(data):${data}","debug")
        if (data) {
            def dev = data.device
            def newState = data.newState.toString() == "1" ? 'on' : 'off'
            logger("SetCircuitCallback-Sending:${newState} to ${dev}","debug")
            dev.sendEvent([name:'switch', value: newState , textDescription: "Set to ${newState}"])
        }
    } else {
        logger("Ciurcuit update failed with code ${response.getStatus()}","error")
    }
}

// **********************************
// INTERNAL Methods
// **********************************
def addHESTChildDevice(namespace, deviceType, dni, options  ) {
	return addChildDevice(namespace, deviceType, dni, options)
}

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
    logger("Send PUT to ${message} with ${body} and ${aCallback}","debug")
     def params = [
        	uri: getControllerURI(),
        	path: message,
        	requestContentType: "application/json",
        	contentType: "application/json",
        	body:body
    	]
     asynchttpPut(aCallback, params, data)

}

def generalCallback(response, data) {
   logger("Callback(status):${response.getStatus()}","debug")
}



def toIntOrNull(it) {
   return it?.isInteger() ? it.toInteger() : null
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
