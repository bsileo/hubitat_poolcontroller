/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Intellibrite
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pool Controller Intellibrite",
            namespace: "bsileo",
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_intellibrite.groovy') {

        capability "Switch"
        capability "Configuration"
        capability "Refresh"
        attribute "swimDelay", "Boolean"
        attribute "lightingTheme", "String"
        attribute "action", "String"

         if (isHE) {
            command "setLightMode", [[name:"Light mode*",
			    "type":"ENUM","description":"Select an Intellibright mode to set",
			    "constraints":["Party", "Romance","Caribbean","American","Sunset","Royal","Blue","Green","Red","White","Magenta"]
		      ]]
         } else {
              // ST version of commands goes here

         }

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
	log.debug("Installed Intellibrite Color Light " + device.deviceNetworkId)
    manageData()
    manageChildren()
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
    getHubPlatform()
}

def updated() {
  manageData()
  manageChildren()
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}


def configure() {
    refreshConfiguration(true)
    getHubPlatform()
}

def refreshConfiguration(process = false) {
    def cid = getDataValue('circuitID')
    def aCallback = 'parseConfiguration'
    def body = ''
    if (process) {
        aCallback = 'configurationCallback'
    def params = [
        uri: getParent().getControllerURI(),
        path: "/config/circuit/${cid}/lightThemes",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    asynchttpGet(aCallback, params, data)
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
    if (response.getStatus() == 200) {
        def msg = response.json
        logger(msg,"trace")
        state.colors = msg
        return true
    } else {
        logger("Configuration Failed with code ${response.getStatus()}","error")
        return false
    }
}

def manageData() {
 	def cid = getDataValue("circuitID")
	sendEvent(name: "circuitID", value: cid, isStateChange: true, displayed: false)
}

def manageChildren() {
	def hub = location.hubs[0]

    def colors = state.colors

    def skipModes = ['unknown','save','reset','none','colorset','colorswim','recall','reset','hold']

 	def displayName
    def deviceID
    def existingButton
    def cDNI

	// Create selected devices
	colors.each {
        if (! skipModes.contains(it.name)) {
            logger("Process ${it} ${it.val}--${it.name}->${it.desc}","trace")
            displayName = "Intellibrite ${it.desc} mode"
            deviceID = "intellibrite-${it.name}"
            dni = device.deviceNetworkId + "-${it.name}"
            existing = childDevices.find({it.deviceNetworkId == dni})
            if (!existing){
                try{
                    logger("Creating ${it.desc} light mode button","debug")
                   	def cButton = addChildDevice("hubitat", "Generic Component Switch", dni,
                        [ label: displayName,
                         componentName: deviceID,
                         componentLabel: displayName,
                         isComponent:true,
                         completedSetup:true,
                         modeName: it.name,
                         modeVal: it.val.toString()
                        ])
                    logger( "Created Button for ${it.name} ","info")
                }
                catch(e)
                {
                  logger( "Error! problem creating light mode device. Check your hub to make sure the 'Generic Component Switch is available - " + e ,"error")
                }
           } else {
               logger( "Existing Button for ${it} Updated","info")
               existing.updateDataValue("modeName",it.name)
               existing.updateDataValue("modeVal",it.val.toString())
           }
        } else {
            logger("Skipped ${it} ${it.val}--${it.name}->${it.desc}","trace")
        }
   }
}


def parse(msg) {
    logger("Parse Intellibrite - ${msg}","trace")
    logger("Implement pare Intellibrite - ${msg}","error")
}

def refresh() {
    logger("refresh Intellibrite - ${msg}","trace")
    def body = null
    def params = [
        uri: getParent().getControllerURI(),
        path: "/config/lightGroup/colors",
        requestContentType: "application/json",
        contentType: "application/json",
        body:body
    ]
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh (response, data) {
    logger("Parse Refresh ${response.getStatus()} -- ${response.getStatus()==200}","debug")
    if (response.getStatus() == 200) {
        def json = response.getJson()
        logger("Parse Refresh JSON ${json}","debug")
        sendEvent([[name: "swimDelay", value: json.swimDelay ? true : false]])
        sendEvent([[name: "lightingTheme", value: json.lightingTheme.name, descriptionText:"Lighting Theme is ${json.lightingTheme.desc}"]])
        sendEvent([[name: "action", value: json.action.name, descriptionText:"Lighting Action is ${json.action.desc}"]])
        json.circuits.each {
            logger("Circuits ${it.circuit.id} ${getDataValue('circuitID')}","trace")
            if (it.circuit.id.toString() == getDataValue("circuitID").toString()) {
                 sendEvent([[name: "switch", value: it.circuit.isOn ? 'On' : 'Off', descriptionText:"Light switch is ${it.circuit.isOn ? 'On' : 'Off'}"]])
            }
        }
    } else {
        logger("Refresh Failed with code ${response.getStatus()}","error")
    }
}


def setLightState(state) {
    def id = getDataValue("circuitID")
    def body = [id: id, state: state]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/circuit/setState",
        body: body,
        requestContentType: "application/json",
        contentType: "application/json"
    ]
    logger("Set Intellibrite mode with ${params} - ${data}","debug")
    asynchttpPut('lightModeCallback', params, data)
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
}

def on() {
    setLightState(1)
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
}


def off() {
    setLightState(0)
	sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
}



def componentRefresh(device) {
    logger("Got REFRESH Request from ${device}","debug")

}

def componentOn(device) {
	logger("Got ON Request from ${device}","debug")
    def mode = device.getDataValue("modeName")
    def modeVal = device.getDataValue("modeVal")
	setLightModeByVal(modeVal)
    device.off()
}

def componentOff(device) {
	logger( "Got OFF from ${device}","debug")
	// Noop - we only operate on the ON cycle
}


def setLightMode(mode) {
    def child = childDevices.find { it -> it.getDataValue("modeName") == mode.toLowerCase() }
    logger("Getting value from child ${child}","trace")
    if (child) {
        def modeVal = child.getDataValue("modeVal")
	    setLightModeByVal(modeVal)
    }
}

def setLightModeByVal(modeVal) {
    logger("Going to light mode ${modeVal}","debug")
    def id = getDataValue("circuitID")
    def body = [theme: modeVal]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/intellibrite/setTheme",
        requestContentType: "application/json",
        contentType: "application/json",
        body: body
    ]
    logger("Set Intellibrite mode with ${params} - ${data}","debug")
    asynchttpPut('lightModeCallback', params, data)
    sendEvent(name: "switch", value: "on")
}

def lightModeCallback(response, data) {
    logger("LightMode Result ${response.getStatus()}","debug")
    logger("LightMode Response Data ${response.getData()}","debug")
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

