/**
 *  Copyright 2018 Brad Sileo
 *
 *
 *  Intellibrite Color Mode Tile
 *
 *  Author: Brad Sileo
 * 
 */
metadata {
	definition (name: "Pentair Intellibrite Color Light",
            namespace: "bsileo", 
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_intellibrite_color_light.groovy') {		
		capability "Switch"
		capability "Momentary"      
		command "setLightMode", [[name:"Light mode*",
			"type":"ENUM","description":"Select an Intellibright mode to set",
			"constraints":["Party", "Romance","Caribbean","American","Sunset","Royal","Blue","Green","Red","White","Magenta"]
			]]
	}
}


def configure() {

}

def installed() {
	log.debug("Installed Intellibrite Color Light " + device.deviceNetworkId)
    manageData()
    manageChildren()       	
}

def updated() {
  manageData()
  manageChildren()
}

def manageData() {
 	def cid = getDataValue("circuitID")
	sendEvent(name: "circuitID", value: cid, isStateChange: true, displayed: false)
}

def getlightModeChild(mode) {
	def instanceID = getDataValue("instanceID")
    return getParent().getIntellibrightChild(instanceID, mode)
}

def manageChildren() {
	def hub = location.hubs[0]    
	log.debug "TODO - Connect to Intellibrite Light Mode Children for this device"
	//def colors = ['Off','On','Color Sync','Color Swim','Color Set', 'Party','Romance','Caribbean','American','Sunset','Royal','Save','Recall','Blue','Green','Red','White','Magenta']
    def colors = ['Party','Romance','Caribbean','American','Sunset','Royal','Green','Red','White','Magenta','Blue']
        
 	def displayName
    def deviceID
    def existingButton
    def cDNI
    def circuitID = getDataValue("circuitID")
	// Create selected devices
	colors.each {
    	log.debug ("Create " + it + " light mode button")
 	    displayName = "IBM-${circuitID}-" + it
        deviceID = it
        existingButton = getParent().childDevices.find({it.deviceNetworkId == parent.getChildDNI(deviceID)})
        // TODO - need to register with this device here - "subscribe" via a smartapp to update my modeName correctly
      }
}


def parse(String description) {
}

def on() {
	sendEvent(name: "switch", value: "on", isStateChange: true, displayed: true)
    parent.setCircuit(getDataValue("circuitID"), 1)
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true, displayed: true)
    parent.setCircuit(getDataValue("circuitID"), 0)
}

def onConfirmed() {
    //log.debug("CONF ${device} turned on")
	sendEvent(name: "switch", value: "on", displayed:true)    
}

def offConfirmed() {
	//log.debug("CONF ${device} turned off")
	sendEvent(name: "switch", value: "off", displayed:true)  
}

def setLightMode(mode) {
	def child = getlightModeChild(mode)
    log.debug("Pushing ${child}")
	return child?.push()
}

def setCircuitFunction(f) {
    state.circuitFunction=f
}

def setFriendlyName(name) {
    state.friendlyName=name
}