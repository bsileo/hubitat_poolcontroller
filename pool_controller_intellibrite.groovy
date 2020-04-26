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

        command "setLightMode", [[name:"Light mode*",
			"type":"ENUM","description":"Select an Intellibright mode to set",
			"constraints":["Party", "Romance","Caribbean","American","Sunset","Royal","Blue","Green","Red","White","Magenta"]
		  ]]
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
    manageChildren()
}

def installed() {
	log.debug("Installed Intellibrite Color Light " + device.deviceNetworkId)
    manageData()
    manageChildren()
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def updated() {
  manageData()
  manageChildren()
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def manageData() {
 	def cid = getDataValue("circuitID")
	sendEvent(name: "circuitID", value: cid, isStateChange: true, displayed: false)
}

def manageChildren() {
	def hub = location.hubs[0]

	//def colors = ['Off','On','Color Sync','Color Swim','Color Set', 'Party','Romance','Caribbean','American','Sunset','Royal','Save','Recall','Blue','Green','Red','White','Magenta']
    def colors = ['Party','Romance','Caribbean','American','Sunset','Royal','Green','Red','White','Magenta','Blue']

 	def displayName
    def deviceID
    def existingButton
    def cDNI

	// Create selected devices
	colors.each {
    	logger("Create " + it + " light mode button","debug")
 	    displayName = "Intellibrite " + it + " mode"
        deviceID = "intellibrite-${it}"
        dni = device.deviceNetworkId + "-${it}"
        existing = childDevices.find({it.deviceNetworkId == dni})
        if (!existing){
            try{
               	def cButton = addChildDevice("hubitat", "Generic Component Switch", dni,
                    [ label: displayName,
                     componentName: deviceID,
                     componentLabel: deviceID,
                     isComponent:true,
                     completedSetup:true,
                     modeName: it
                    ])
                logger( "Created Button for ${it} ","info")
            }
            catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
            {
              logger( "Error! problem creating light mode device. Check your hub to make sure the 'Generic Component Switch is available - " + e ,"error")
            }
       } else {
           logger( "Existing Button for ${it} updated","trace")
           existing.updateDataValue("modeName",it)
       }
   }
}


def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)

}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)

}

def componentRefresh(device) {
    logger("Got REFRESH Request from ${device}","debug")

}

def componentOn(device) {
	logger("Got ON Request from ${device}","debug")
    def mode = device.getDataValue("modeName")
	setLightMode(mode)
    device.off()
}

def componentOff(device) {
	logger( "Got OFF from ${device}","debug")
	// Noop - we only operate on the ON cycle
}


def setLightMode(mode) {
    logger("Going to light mode ${mode}","debug")
    def id = getDataValue("circuitID")
    def data = [theme: mode]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/intellibrite/setTheme"
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