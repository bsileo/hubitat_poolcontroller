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

		attribute "circuitID", "Number"

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
    	tiles {
            standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
                state "off", label: "Off", action: "on", icon:"st.Lighting.light21", nextState: "on", backgroundColor: "#ffffff"
                state "on", label: "On", action: "off", icon:"st.Lighting.light21",  nextState: "off", backgroundColor: "#79b821"
			}
            
            standardTile("theme", "lightingTheme", width: 2, height: 2, canChangeIcon: true) {
                state "party", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/party.png", backgroundColor:"#4250f4", nextState:"off"
                state "romance", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/romance.png", backgroundColor:"#d28be8", nextState:"off"
                state "caribbean", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/caribbean.png", backgroundColor:"#46f2e9", nextState:"off"        
                state "american", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/american.png", backgroundColor:"#d42729", nextState:"off"        
                state "sunset", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/sunset.png", backgroundColor:"#ffff00", nextState:"off"        
                state "royal", label:"", action:"off", icon:"https://bsileo.github.io/SmartThings_Pentair/royal.png", backgroundColor:"#9933ff", nextState:"off"        

                state "blue", label:"Blue", action: "off", icon:"st.Lighting.light21", backgroundColor:"#0000FF", nextState:"off"
                state "green", label:"Green", action: "off", icon:"st.Lighting.light21", backgroundColor:"#33cc33", nextState:"off"
                state "red", label: "Red", action: "off", icon:"st.Lighting.light21",backgroundColor: "#bc3a2f", nextState: "off"
                state "white", label:"White", action:"off", icon:"st.Lighting.light21", backgroundColor:"#ffffff", nextState:"off"
                state "magenta", label:"Magenta", action:"off", icon:"st.Lighting.light21", backgroundColor:"#ff00ff", nextState:"off"            
            }           
            
            
            standardTile("refresh", "refresh", width:1, height:1, inactiveLabel: false, decoration: "flat") {
				state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
			}

            main "switch"
            details "switch", "theme", "refresh"
		}
    }
}

def installed() {
	log.debug("Installed Intellibrite Color Light " + device.deviceNetworkId)
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Info'
    getHubPlatform()
    manageData()
    manageChildren()
}

def updated() { 
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE : 'Info'
  manageData()
  refreshConfiguration(true)
}


def configure() {
	getHubPlatform()
    refreshConfiguration(true) 
}

def refreshConfiguration(process = false) {    
    def cid = getDataValue("circuitID")
    def body = ''
    def data = null
    def aCallback = 'parseConfiguration'
    if (process) {
        aCallback = 'configurationCallback'
    }
    logger("Resfresh Config for Circuit ${cid}","debug")
    sendGet("/config/lightGroups/themes", aCallback, body, data)
}


def configurationCallback(response, data=null) {
    if (parseConfiguration(response, data)) {
        manageChildren()
    } else {
        logger("Failed to process configuration ${response}","error")
    }
}

def parseConfiguration(response, data=null) {
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
    logger("Process manageChildren for ${colors}","trace")
    def skipModes = ['unknown','save','reset','none','colorset','colorswim','recall','reset','hold']

 	def displayName
    def deviceID
    def existing
    def dni

	def namespace = state.isHE ? 'hubitat' : 'smartthings'
    def deviceType = state.isHE ? "Generic Component Switch" : "Virtual Switch"
    
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
                    logger("Namespace ${namespace} type ${deviceType}  ${dni}  with Name= ${it.name} and val=${it.val}","debug")
                   	def cButton = addHESTChildDevice(namespace, deviceType, dni,
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
                  logger( "Error! problem creating light mode device. Check your logs for more details - " + e ,"error")
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
    logger("Implement parsee Intellibrite - ${msg}","error")
}

def refresh() {
    logger("refresh Intellibrite - ${msg}","trace")
    def body = null
    def data = null
    sendGet("/config/lightGroup/colors", 'parseRefresh', body, data)    
}

def parseRefresh (response, data=null) {
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
    logger("Set Intellibrite mode with ${params} - ${data}","debug")
    sendPut("/state/circuit/setState", 'lightModeCallback', body, data)
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
    logger("Set Intellibrite mode with ${params} - ${data}","debug")
    sendPut("/state/intellibrite/setTheme",'lightModeCallback', body, data)
    sendEvent(name: "switch", value: "on")
}

def lightModeCallback(response, data=null) {
    logger("LightMode Result ${response.getStatus()}","debug")
    logger("LightMode Response Data ${response.getData()}","debug")
}


// **********************************
// INTERNAL Methods
// **********************************
def addHESTChildDevice(namespace, deviceType, dni, options  ) {
	if (state.isHE) {
    	return addChildDevice(namespace, deviceType, dni, options)
	} else {    	
    	return addChildDevice(namespace, deviceType, dni, location.hubs[0]?.id, options)
    }
}

// INTERNAL Methods
private getHost() {
    return getParent().getHost()
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
    if (state.isST) {
    	 def hubAction = physicalgraph.device.HubAction.newInstance(
               [
                method: "GET",
                path: message,
                body: body,
                headers: [
                    HOST: getHost(),
                    "Accept":"application/json"
                    ]
               ],
               null,
               [    
                callback : aCallback,
                type: 'LAN_TYPE_CLIENT'
               ])            
        sendHubCommand(hubAction)
    } else {    	
        asynchttpGet(aCallback, params, data)
    }    
}

private sendPut(message, aCallback=generalCallback, body="", data=null) {    
    logger("Send PUT to ${message} with ${params} and ${aCallback}","debug")
    if (state.isST) {         
        def hubAction = physicalgraph.device.HubAction.newInstance(
               [
                method: "PUT",
                path: message,
                body: body,
                headers: [
                    HOST: getHost(),
                    "Accept":"application/json"
                    ]
               ],
               null,
               [    
                callback : aCallback,
                type: 'LAN_TYPE_CLIENT'
               ])            
        sendHubCommand(hubAction)        
    } else {
     	def params = [
        	uri: getControllerURI(),
        	path: message,
        	requestContentType: "application/json",
        	contentType: "application/json",
        	body:body
    	]
        asynchttpPut(aCallback, params, data)
    }
    
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

