/**
 *  Copyright 2019 Brad Sileo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

metadata {
	definition (name: "Pentair Pool Controller", namespace: "bsileo", author: "Brad Sileo", importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_pool_controller.groovy') {
       capability "Polling"
       capability "Refresh"
       capability "Configuration"
       capability "Switch"
       capability "Actuator"
       capability "Sensor"
       attribute "poolPump","string"
       attribute "spaPump","string"
       attribute "valve","string"       
       command "poolPumpOn"
       command "poolPumpOff"
       command "spaPumpOn"
       command "spaPumpOff"
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
  logger( "Executing 'configure()'","info")
  updateDeviceNetworkID()
}

def installed() {
	manageChildren()    
}

def updated() {
  manageChildren()
  if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 5000) {
    state.updatedLastRanAt = now()
    logger( "Executing 'updated()'","debug")
    runIn(3, "updateDeviceNetworkID")
  } else {
    log.trace "updated(): Ran within last 5 seconds so aborting."
  }  
}

def manageChildren() {
	logger( "Pool Controller manageChildren...","debug")
   
    def poolHeat = childDevices.find({it.deviceNetworkId == getChildDNI("poolHeat")})
    if (!poolHeat) {
        poolHeat = addChildDevice("bsileo","Pentair Water Thermostat", getChildDNI("poolHeat"),  
                                  [completedSetup: true, label: "${device.displayName} (Pool Heat)" , isComponent:false, componentName: "poolHeat", componentLabel:"${device.displayName} (Pool Heat)" ])
        logger( "Created PoolHeat" ,"debug")
    }
    if (getDataValue("includeSpa")=='true') {
        def spaHeat = childDevices.find({it.deviceNetworkId == getChildDNI("spaHeat")})
        if (!spaHeat) {
            spaHeat = addChildDevice("bsileo","Pentair Water Thermostat", getChildDNI("spaHeat"),  
                                     [completedSetup: true, label: "${device.displayName} (Spa Heat)" , isComponent:false, componentName: "spaHeat", componentLabel:"${device.displayName} (Spa Heat)" ])
            logger( "Created SpaHeat","debug")
        }
        def spaPump = childDevices.find({it.deviceNetworkId == getChildDNI("spaPump")})
        if (!spaPump) {
            spaHeat = addChildDevice("bsileo","Pentair Spa Pump Control", getChildDNI("spaPump"),  
                                     [completedSetup: true, label: "${device.displayName} (Spa Pump)" , isComponent:false, componentName: "spaPump", componentLabel:"${device.displayName} (Spa Pump)" ])
            logger( "Created SpaPump Child","debug")
        }
    }    
    manageIntellibriteLights()
    managePumps()
    manageCircuits()


    def airTemp = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
    if (!airTemp) {
        airTemp = addChildDevice("bsileo","Pentair Temperature Measurement Capability", getChildDNI("airTemp"),  
                                 [ label: "${device.displayName} Air Temperature", componentName: "airTemp", componentLabel: "${device.displayName} Air Temperature",
                                  isComponent:false, completedSetup:true])                	
    }

    
    if (getDataValue("includeSolar")=='true') {    
    	def solarTemp = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})        
    	if (!solarTemp) {
    		logger(("Create Solar temp"),"debug")
        	solarTemp = addChildDevice("bsileo","Pentair Temperature Measurement Capability", getChildDNI("solarTemp"),  
                                   [ label: "${device.displayName} Solar Temperature", componentName: "solarTemp", componentLabel: "${device.displayName} Solar Temperature",
                                    isComponent:false, completedSetup:true])        
    	}
    }
    else {
    	 // Nothing needed...         
    }
    
    

    def ichlor = childDevices.find({it.deviceNetworkId == getChildDNI("poolChlorinator")})
    if (!ichlor && getDataValue("includeChlorinator")=='true') {
    	logger(("Create Chlorinator"),"debug")
        ichlor = addChildDevice("bsileo","Pentair Chlorinator", getChildDNI("poolChlorinator"), 
                                [ label: "${device.displayName} Chlorinator", componentName: "poolChlorinator", componentLabel: "${device.displayName} Chlorinator",
                                 isComponent:true, completedSetup:true])        
    }  
    def ichem = childDevices.find({it.deviceNetworkId == getChildDNI("poolIntellichem")})
    if (!ichem && getDataValue("includeIntellichem")=='true') {          
        ichem = addChildDevice("bsileo","Pentair Intellichem", getChildDNI("poolIntellichem"), 
                               [ label: "${device.displayName} Intellichem", componentName: "poolIntellichem", componentLabel: "${device.displayName} Intellichem",
                                isComponent:false, completedSetup:true])  
    }   
}

def manageIntellibriteLights() {
  
	logger( "Create/Update Intellibrite Light Children for this device","debug")
    def lights = getParent().getState().lightCircuits
    def instance = 1
    def lCircuits = getParent().getState().circuitData
    state.intellibriteInstances = [:]
    lights.each { circuitID, fName ->
	    def lightInfo = lCircuits[circuitID]
    	if (lightInfo['circuitFunction'] == "Intellibrite") {
    		state.intellibriteInstances[instance] = circuitID
        	state.intellibriteInstances[circuitID] = instance
			makeIntellibriteLightCircuit(circuitID,instance)
            manageIntellibriteModes(instance,fName, circuitID)
        }
        else {
        	makeLightCircuit(circuitID)
        }
        instance = instance+1
     }
}

def makeLightCircuit(circuitID) {

    def lCircuits = getParent().getState().circuitData
    def lightInfo = lCircuits[circuitID]
    def auxname = "circuit${circuitID}"        
    def auxLabel = "${device.displayName} (${lightInfo.circuitName})"        
    try {
            def auxButton = childDevices.find({it.deviceNetworkId == getChildDNI(auxname)})
            if (!auxButton) {
            	log.info "Create Light switch ${auxLabel} Named=${auxname}" 
                auxButton = addChildDevice("bsileo","Pentair Pool Light Switch", getChildDNI(auxname),
                                           [completedSetup: true, label: auxLabel , isComponent:false, componentName: auxname, componentLabel: auxLabel,
                                           data:[circuitID:circuitID]
                                           ])
                logger( "Success - Created Light switch ${circuitID}" ,"debug")
            }
            else {
                log.info "Found existing Light Switch ${circuitID} - No Updates Supported" 
            }
        }
        catch(e)
        {
            logger( "Error! " + e ,"debug")                                                         
        }
}

def makeIntellibriteLightCircuit(circuitID,instance) {
    def lCircuits = getParent().getState().circuitData
    def lightInfo = lCircuits[circuitID]
    def auxname = "IB${instance}-Main"        
    def auxLabel = "${device.displayName} (${lightInfo.name})"        
    try {
            def auxButton = childDevices.find({it.deviceNetworkId == getChildDNI(auxname)})
            if (!auxButton) {
            	log.info "Create Light switch ${auxLabel} Named=${auxname}" 
                auxButton = addChildDevice("bsileo","Pentair Intellibrite Color Light", getChildDNI(auxname),  
                                           [completedSetup: true, label: auxLabel , isComponent:false, componentName: auxname, componentLabel: auxLabel,
                                           data:[circuitID:circuitID, instanceID:instance]
                                           ])
                auxButton.updateDataValue("circuitID",circuitID)
                auxButton.updateDataValue("instanceID",instance)
                logger( "Success - Created Intellibrite Light switch ${instance}=${circuitID}" ,"debug")
            }
            else {
                log.info "Found existing Light Switch ${circuitID} - refreshed" 
                auxButton.updateDataValue("circuitID",circuitID)
                auxButton.updateDataValue("instanceID",instance)
            }
        }
        catch(e)
        {
            logger( "Error! " + e  ,"debug")                                                              
        }
}
def manageIntellibriteModes(instanceID, fName, circuitID) {	  
	logger( "Create/Update Intellibrite Light Mode Children for device:" + circuitID,"debug")
	//def colors = ['Off','On','Color Sync','Color Swim','Color Set', 'Party','Romance','Caribbean','American','Sunset','Royal','Save','Recall','Blue','Green','Red','White','Magenta']
    def colors = ['Party','Romance','Caribbean','American','Sunset','Royal','Green','Red','White','Magenta','Blue']
        
 	def displayName
    def deviceID
    def existingButton
    def cDNI    
	// Create selected devices
	colors.each {
    	logger( ("Create " + it + " light mode button"),"debug")
 	    displayName = "Intellibrite Circuit ${instanceID}:${it}"
        deviceID = "lightmode-${instanceID}-${it}"
        cDNI = getChildDNI(deviceID)
        existingButton = childDevices.find({it.deviceNetworkId == cDNI})        
        logger( ("Create " + it + " ${displayName}::${deviceID}==${cDNI}" ),"debug")
        if (!existingButton){                
                try{                           
                	def cButton = addChildDevice("bsileo", "Pentair Intellibrite Color Light Mode", cDNI,
                             [ label: displayName, componentName: deviceID, componentLabel: deviceID,
                             isComponent:true, completedSetup:true,
                             data: [modeName:it, circuitID:circuitID]
                             ])
                    cButton.updateDataValue("circuitID",circuitID)
                    cButton.updateDataValue("modeName",it)
                	state.installMsg = state.installMsg + displayName + ": created light mode device. \r\n\r\n"
                }
                catch(e)
                {
                    logger( "Error! " + e ,"debug")                                    
                    state.installMsg = state.installMsg + it + ": problem creating light mode device. Check your IDE to make sure the smartthings : Pentair Intellibrite Light Mode device handler is installed and published. \r\n\r\n"
                }
            }
            else {
                state.installMsg = state.installMsg + it + ": light mode device already exists. \r\n\r\n"
                logger( "Existing button: " + existingButton,"debug")
                existingButton.updateDataValue("circuitID",circuitID)
                existingButton.updateDataValue("modeName",it)
            }
      }
}


def managePumps() {
	logger( "Create/Update Pumps for this device","debug")
	def hub = location.hubs[0]   
    def pumps = getParent().getState().pumps
    pumps.each {id,data ->
    	try {
        	if (data['type'] != 'none') {
                def pumpName = "PumpID${id}"
                def pumpFName = "Pump # ${id}"
                def childDNI = getChildDNI(pumpName)
                def pump = childDevices.find({it.deviceNetworkId == childDNI})
                if (!pump) {
                    log.info "Create Pump Controller Named=${pumpName}" 
                    pump = addChildDevice("bsileo","Pentair Pump Control", childDNI,  
                                               [completedSetup: true, label: pumpFName , isComponent:false, componentName: pumpName, componentLabel: pumpName, 
                                               data: [
                                                type: data['type'],
                                                friendlyName: data['friendlyName'],
                                                pumpID: id,
                                                circuitID: 6,
                                                externalProgram: data['externalProgram']
                                                ]
                                               ])
                    logger( "Success - Created Pump ID ${id}" ,"debug")
                }
            }
        }
        catch(e)
        {
            logger( "Error With Pump Child ${id} - " + e  ,"debug")                                                              
        }
    }
}

def manageCircuits() {
	logger( "Create/Update Circuits for this device","debug")
	manageFeatureCircuits()
}


def manageFeatureCircuits() {
	def hub = location.hubs[0]   
    def nLCircuits = getParent().getState().nonLightCircuits
    nLCircuits.each {i,k ->
    	def cData = getParent().getState().circuitData[i.toString()]
        if (cData.friendlyName == "NOT USED") return        
        def auxname = "circuit${i}"        
        def auxLabel = "${device.displayName} (${cData.friendlyName})"        
        try {
            def auxButton = childDevices.find({it.deviceNetworkId == getChildDNI(auxname)})
            if (!auxButton) {
            	log.info "Create Aux Circuit switch ${auxLabel} Named=${auxname}" 
                auxButton = addChildDevice("bsileo","Pentair Pool Control Switch", getChildDNI(auxname),  
                                           [completedSetup: true, label: auxLabel , isComponent:false, componentName: auxname, componentLabel: auxLabel, 
                                           data: [type:cData.circuitFunction]
                                           ])
                logger( "Success - Created Aux switch ${i}" ,"debug")
            }
            else {
                log.info "Found existing Aux Switch ${i} - No Updates Supported" 
            }
        }
        catch(e)
        {
            logger( "Error! " + e ,"debug")                                                               
        }
    }
}


def refresh() {
    log.info "Requested a refresh"
    poll()
}

def poll() {
  sendEthernet("/all")
}

def parse(String description) {  
  logger( "Executing parse()","trace")
  def msg = parseLanMessage(description)
  logger( "Full msg: ${msg}","trace")
  logger( "HEADERS: ${msg.headers}","trace")
  logger( "JSON: ${msg.json}","trace")
  logger( "x-event: ${msg.headers['x-event']}","trace")
  logger( "msg.JSON.Circuits: ${msg.json.circuit}","trace")
  logger( "msg.JSON.Time: ${msg.json.time}","trace")
  logger( "msg.JSON.Temp: ${msg.json.temperature}","trace")
  logger( "msg.JSON.Chem: ${msg.json.intellichem}","trace")
  if (msg.json) {
      if (msg.json.temperature != null) {parseTemps(msg.json.temperature)} else {logger("no Temps in msg","trace")}
      if (msg.json.circuit != null){ parseCircuits(msg.json.circuit)} else {logger("no Circuits in msg","trace")}
      if (msg.json.time != null) {parseTime(msg.json.time)} else {logger("no Time in msg","trace")}
      if (msg.json.schedule != null) {parseSchedule(msg.json.schedule)} else {logger("no Schedule in msg","trace")}
      if (msg.json.pump != null) {parsePump(msg.json.pump)} else {logger("no Pumps in msg","trace")}
      if (msg.json.valve != null) {parseValve(msg.json.valve)} else {logger("no Valve in msg","trace")}     
      if (msg.json.chlorinator != null) {parseChlorinator(msg.json.chlorinator)} else {logger("No Chlor in msg","trace")}
      if (msg.json.intellichem != null) {parseIntellichem(msg.json.intellichem)} else {logger("no Chem in msg","trace")}
  }
  else {
     logger( "No JSON In response MSG: ${msg}","debug")
  }
}

def parseTime(msg) {
	logger("Parse Time: ${msg}","debug")
}
def parsePump(msg) {
	logger("Parse Pump: ${msg}","debug")
    msg.each { key, value ->    
    	def id = key
    	def pumpName = "PumpID${id}"
        def pumpDNI = getChildDNI(pumpName)
    	childDevices.find({it.deviceNetworkId == pumpDNI})?.parsePumpData(value)
    }
}
def parseSchedule(msg) {
	log.info("Parse Schedule: ${msg}")
}
def parseValve(msg) {
	log.info("Parse Valve: ${msg}")
    sendEvent(name: "valve", value: msg.valves)            
}
def parseIntellichem(msg) {
	log.info("Parse Intellichem: ${msg}")
    childDevices.find({it.deviceNetworkId == "poolIntellichem"})?.parse(msg)
}
 

def parseCircuits(msg) {   
	logger("Parse Circuits: ${msg}","debug")
    msg.each {
         def child = getChildCircuit(it.key)
         //logger( "CIR JSON:${it.key}==${it.value}::${child}","debug")
         if (child) {
            def stat = it.value.status ? it.value.status : 0         
            def status = stat == 0 ? "off" : "on"
            //logger( "Child=${it.key}=${child} --> ${stat}","debug")
            def currentID = toIntOrNull(it.key)
         	if (stat == 0) { 
                child.offConfirmed() 
             } 
            else { 
               child.onConfirmed()
            };
            if (currentID == poolPumpCircuitID()) { 
                sendEvent(name: "poolPump", value: status, displayed:true)            
            }
            if (currentID == spaPumpCircuitID()) { 
            	sendEvent(name: "spaPump", value: status, displayed:true)
                def spaPump = getSpaPumpChild()
                if (stat == 0) { 
                	spaPump.offConfirmed() 
             	} 
            	else { 
               		spaPump.onConfirmed()
            	};
            }
     		child.setCircuitFunction("${it.value.circuitFunction}")
            child.setFriendlyName("${it.value.friendlyName}")               

            sendEvent(name: "circuit${currentID}", value:status, 
             				displayed:true, descriptionText:"Circuit ${child.label} set to ${status}" 
                            )            
  
         }
      }     
}

def getChildCircuit(id) {
	// get the circuit device given the ID number only (e.g. 1,2,3,4,5,6)
    logger( "CHECK getChildCircuit:${id}","trace")
	def children = getChildDevices()
    def cname = 'circuit' + id
    def instance = state.intellibriteInstances[id]
    if (instance) {    	
    	cname = "IB${instance}-Main"
        logger( "IB Light${id}==${cname}","trace")
    }    
	def dni = getChildDNI(cname)
    //return childDevices.find {it.deviceNetworkId == dni}
    
    def theChild
    children.each { child ->
    	logger( "CHECK Child for :${dni}==${child}::" + child.deviceNetworkId,"trace")
        if (child.deviceNetworkId == dni) { 
          logger( "HIT Child for :${id}==${child}","trace")
          theChild = child          
        }
    }
    return theChild
    
}

def getPoolPumpChild() {
	return childDevices.find({it.deviceNetworkId == getChildDNI("poolPump")})
}

def getPoolHeatChild() {
	return childDevices.find({it.deviceNetworkId == getChildDNI("poolHeat")})
}


def getSpaPumpChild() {
	return childDevices.find({it.deviceNetworkId == getChildDNI("spaPump")})
}

def getSpaHeatChild() {
	return childDevices.find({it.deviceNetworkId == getChildDNI("spaHeat")})
}


def getChildDNI(name) {
	return getDataValue("controllerMac") + "-" + name
}

def parseTemps(msg) {
    log.info("Parse Temps ${msg}")
    def ph=childDevices.find({it.deviceNetworkId == getChildDNI("poolHeat")})
    def sh=childDevices.find({it.deviceNetworkId == getChildDNI("spaHeat")})
    def at = childDevices.find({it.deviceNetworkId == getChildDNI("airTemp")})
    def st = childDevices.find({it.deviceNetworkId == getChildDNI("solarTemp")})
    
    msg.each {k, v ->        	         
         //logger( "TEMP Key:${k}  Val:${v}","debug")
         switch (k) {
        	case "poolTemp":            	
            	ph?.setTemperature(v)
            	break
        	case "spaTemp":
            	sh?.setTemperature(v)
            	break
        	case "airTemp":            	
                at?.setTemperature(v)
            	break
        	case "solarTemp":
                st?.setTemperature(v)
            	break
        	case "poolSetPoint":            	
                ph?.setHeatingSetpoint(v)
            	break
            case "spaSetPoint":
            	sh?.setHeatingSetpoint(v)
            	break
        	case "poolHeatMode":
                ph?.switchToModeID(v)                            	
                break
            case "spaHeatMode":
            	sh?.switchToModeID(v)
                break
            default:
            	sendEvent(name: k, value: v, displayed:false)
            	break
          }
	}
}

def parseChlorinator(msg) {
	log.info('Parse Chlor')
    childDevices.find({it.deviceNetworkId == getChildDNI("poolChlorinator")})?.parse(msg)
}

def chlorinatorOn() {  
  return chlorinatorOn(70)
}

def chlorinatorOn(level) {  
  return sendEthernet("/chlorinator/${level}")
}


def chlorinatorOff() {  
  return sendEthernet("/chlorinator/0")
}

// PUMP Control

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

def setPumpSpeed(pumpID, speed) {
	logger( "Set Pump ${pumpID} to Speed ${speed}","debug")
    logger( "TODO - Pump control APIs not working yet","debug")
    //sendEthernet("/pumpCommand/run/pump/${pumpID}/rpm/${speed}", setPumpCallback)
}

def setPumpCallback(hubResponse) {    
	def msg = hubResponse.body
    logger("SetPumpCallback(MSG):${msg}","debug")
    sendEthernet("/pump")
}

//
// Intellibrite color light API interface
//


def setColor(circuitID,colorID) {
    setCircuit(circuitID,1)
    if (colorID > 127)  {
		sendEthernet("/light/mode/${colorID}", setColorCallback)
    }
    else {    
 		sendEthernet("/light/circuit/${circuitID}/setColor/${colorID}", setColorCallback)
    }
}

def setColorCallback(hubResponse) {    
	def msg = hubResponse.body
    //logger("ColorCallback(MSG):${msg}","debug")
    sendEthernet("/circuit")
}

def lightCircuitID() {
	//logger("Get LIGHTS child " + childofType("Intellibrite")?.deviceNetworkId,"debug")   
	return childCircuitID(childofType("Intellibrite")?.deviceNetworkId)
}

def poolPumpCircuitID() {
	//logger("Get Pool child-"+childofType("Pool")?.deviceNetworkId,"debug")
	return childCircuitID(childofType("Pool")?.deviceNetworkId)
}

def spaPumpCircuitID() {
	//logger("Get Spa child-"+childofType("Spa")?.deviceNetworkId,"debug")
	return childCircuitID(childofType("Spa")?.deviceNetworkId)
}

def childofType(type) {
    //return childDevices.find({it.currentFriendlyName == type})
    return childDevices.find({it.currentcircuitFunction == type})
}

def childOn(cir_name) {
	//logger( "Got on Request from ${cir_name}","debug")
    def id = childCircuitID(cir_name)
	return setCircuit(id,1)
}

def childOff(cir_name) {
	//logger( "Got off from ${cir_name}","debug")
	def id = childCircuitID(cir_name)
	return setCircuit(id,0)
}

def childCircuitID(cirName) {
	//logger("CCID---${cirName}","debug")
	return toIntOrNull(cirName?.split('-')?.getAt(1)?.substring(7))
}

def setCircuit(circuit, state) {
  logger( "Executing 'set(${circuit}, ${state})'","debug")
  sendEthernet("/circuit/${circuit}/set/${state}", setCircuitCallback)  
}

def setCircuitCallback(hubitat.device.HubResponse hubResponse) {    
	logger("SetCircuitCallback(JSON):${hubResponse.json}","debug")
    parseCircuits(hubResponse.json)
}

// **********************************
// Heater control functions to update the current heater state / setpoints on the poolController. 
// spdevice is the child device with the correct DNI to use in referecing SPA or POOL
// **********************************
def heaterOn(spDevice) {
  //logger( "Executing 'heater on for ${spDevice}'","debug")
  def tag = spDevice.deviceNetworkId.toLowerCase().split("-")[1]
  sendEthernet("/${tag}/mode/1", heaterModeCallback)
}

def heaterOff(spDevice) {
	//logger( "Executing 'heater off for ${spDevice}'","debug")
    def tag = spDevice.deviceNetworkId.toLowerCase().split("-")[1]
    sendEthernet("/${tag}/mode/0", heaterModeCallback)
}

def heaterSetMode(spDevice, mode) {
    logger( "Executing 'heater going to ${mode} for ${spDevice}'","debug")
  def tag = spDevice.deviceNetworkId.toLowerCase().split("-")[1]
  sendEthernet("/${tag}/mode/${mode}", heaterModeCallback)
}

def updateSetpoint(spDevice,setPoint) {
  	def tag = spDevice.deviceNetworkId.toLowerCase().split("-")[1]
	sendEthernet("/${tag}/setpoint/${setPoint}")
}

def heaterModeCallback(hubResponse) {
    logger( "Entered heaterModeCallback()...","debug")
	def msg = hubResponse.json    
    logger( "Full msg: ${msg}" ,"debug") 
    logger( "Heater status = ${msg.status}"  ,"trace")  
    logger( "${msg.text} -> indexOf:" + msg.text.indexOf('spa'),"trace")
    if (msg.text.indexOf('spa') > 0) {
    	def ph=getSpaHeatChild()
        log.info("Update Spa heater to ${msg.status}")
    	sh?.switchToMode(msg.status)
    }   
    else {
    	def ph=getPoolHeatChild()
        log.info("Update Pool heater to ${msg.status}")
    	ph?.switchToMode(msg.status)
    }
}



// INTERNAL Methods
private sendEthernet(message) {
	sendEthernet(message,null)
}

private sendEthernet(message, aCallback) {
  def ip = getDataValue('controllerIP')
  def port = getDataValue('controllerPort')
  //logger( "Try for 'sendEthernet' http://${ip}:${port}${message}","debug")
  if (ip != null && port != null) {
    log.info "SEND http://${ip}:${port}${message}"
    sendHubCommand(new hubitat.device.HubAction(
        [
         	method: "GET",
         	path: "${message}",
         	//protocol: Protocol.LAN,
         	headers: [
              	HOST: "${ip}:${port}",
              	"Accept":"application/json" 
            	],
        	query:"",
        	body:""
        ],
        null,
        [
        	callback:aCallback
            //type:LAN_TYPE_CLIENT,
            //protocol:LAN_PROTOCOL_TCP
        ]
    ))
  }
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

private getHostAddress() {
	return "${ip}:${port}"
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
