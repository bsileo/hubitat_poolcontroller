/**
 *  Service Manager for attaching Hubitat to a nodejsPoolController
 *
 *  Copyright 2020 Brad Sileo
 *
 */
definition(
		name: "Pool Controller 6",
		namespace: "bsileo",
		author: "Brad Sileo",
		description: "This is App to connect to the nodejs_poolController (version > 6) and create devices to manage it within Hubitat",
		category: "",
		iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health2-icn.png",
		iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health2-icn@2x.png",
		iconX3Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health2-icn@3x.png")


preferences {
    page(name: "home", title: "Pool Controller", content: "appHome")
    page(name: "deviceDiscovery", title: "Pool Controller Device Discovery", content: "deviceDiscovery")
    page(name: "manualPage", title: "Manually enter PoolController")
    page(name: "selectDevice", title: "Select the Controller", content: "selectDevice")
    page(name: "poolConfig", title: "Pool Configuration", content: "poolConfig")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
    addDevice()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
    addDevice()
}

def initialize() {
	unsubscribe()
	unschedule()
    atomicState.subscribed = false
}


def appHome() {
    def install = true
    return dynamicPage(name: "home", title: "NodeJS Pool Controller", refreshInterval: 0, install: false, uninstall: true) {
      section("Device Discovery and Setup", hideable:true, hidden:false) {
         href(name: "deviceDiscovery", title: "", description: "Start Discovery for new devices", required: false, page: "deviceDiscovery")
         href(name: "manualPage", title: "", description: "Tap to manually enter a controller (Optional, if discovery does not work above)", required: false, page: "manualPage")
      }
    }
}

// UPNP Device Discovery Code
def deviceDiscovery() {
    atomicState.config=false
    ssdpSubscribe()
    ssdpDiscover()
	//log.debug("Check Manual device?-- IP:${controllerIP}:${controllerPort}=${controllerMac}")
    if (controllerIP && controllerPort && controllerMac) {
        def cleanMac = controllerMac.toLowerCase().replaceAll(':','').replaceAll('-','')
        if (!state.devices[cleanMac]) {
            log.debug("Add Manual device-- IP:${controllerIP}:${controllerPort}=${controllerMac}")
            state.devices[cleanMac] = [
        				ipaddress:controllerIP,
                        port:controllerPort,
                        verified:false,
                        mac:controllerMac,
                        hub:"",
                        ssdpPath:"/device",
                        ssdpTerm:"urn:schemas-upnp-org:device:PoolController:1",
                        name:"",
                        ssdpNTS:"",
                        uuid:"",
                        mode:"",
                        ssdpUSN:"uuid:806f52f4-1f35-4e33-9299-b827eb3bb77a::urn:schemas-upnp-org:device:PoolController:1"
        				]
        }

    }
    verifyDevices()
    return dynamicPage(name: "deviceDiscovery", title: "Locate Pool Controller...", nextPage: "selectDevice", refreshInterval: 2, install: false, uninstall: true) {
        section("Please wait while we discover your nodejs-poolController. Discovery can take some time...\n\r Click next to proceed once you see it below:", hideable:false, hidden:false) {
            paragraph describeDevices()
            input "refreshDiscovery", "button", title: "Refresh"
	    }
        section("Manual poolController Configuration", hideable:true, hidden:false) {
            href(name: "manualPage", title: "", description: "Tap to manually enter a controller (Optional, if discovery does not work above)", required: false, page: "manualPage")
        }
	}
}

def appButtonHandler(btn) {
   if(btn == "refreshDiscovery") {
      // do whatever you want to do when the button is pressed
      log.debug("Refresh pressed")
      atomicState.subscribed = false
      unsubscribe()
      ssdpSubscribe()
      ssdpDiscover()
   }
}

private String describeDevices() {
    def sorted = getVerifiedDevices()
    // log.debug("SORTED ${sorted}")
    def builder = new StringBuilder()
    builder << '<ul class="device">'
    sorted.each {
        key, device ->
            def ip = getIP(device)
            def port = getPort(device)
            builder << (
                "<li class='device'>${device.name} ${ip}:${port} (${device.mac})</li>"
                )
    }
    builder << '</ul>'
    builder.toString()
}


def manualPage() {
    return dynamicPage(name: "manualController", title: "Enter Controller", nextPage: "deviceDiscovery", refreshInterval: 0, install: false, uninstall: false) {
		section("Controller") {
            input(name:"controllerIP", type: "text", title: "Controller IP Address", required: true, displayDuringSetup: true, defaultValue:"")
          	input(name:"controllerPort", type: "number", title: "Controller Port", required: true, displayDuringSetup: true, defaultValue:"3000")
          	input(name:"controllerMac", type: "text", title: "Controller MAC Address (AA:BB:CC:11:22:33)", required: true, displayDuringSetup: true)
        }
    }
}



def selectDevice() {
    unschedule()
    unsubscribe()
    atomicState.subscribed = false
    def options = [:]
	def devices = getVerifiedDevices()
	devices.each {
    	//log.debug("Processing ${it}-->${it.value}")
        def ip = getIP(it.value)
        def port = getPort(it.value)
        def value = "${it.value.name} ${ip}:${port} (${it.value.mac})"
		def key = it.value.mac
		options["${key}"] = value
	}
    return dynamicPage(name: "selectDevice", title: "Select the Pool Controller...", nextPage: "poolConfig", refreshInterval: 0, install: false, uninstall: true) {
        section("Select your device:", hideable:false, hidden:false) {
            input(name: "selectedDevice", title:"Select A Device", type: "enum", required:true, multiple:false, description: "Tap to choose", params: params,
            	  options: options, submitOnChange: true, width: 6)
		}
     }
}


// nodejs-PoolController configuration functions
def poolConfig() {
    if (state.config) {
    	log.debug("poolConfig STATE=${state}")
    	return dynamicPage(name: "poolConfig", title: "Verify Final Pool Controller Configuration:", install: true, uninstall: false) {
            section("Name") {
                input name:"deviceName", type:"text", title: "Enter the name for your device:", required:true, defaultValue:"Pool"
            }
            section("Please verify the options below") {
              //input name:"numberCircuits", type:"number", title: "How many circuits:", required:true, defaultValue:state.numCircuits
              input name:"includeSpa", type:"bool", title: "Enable Spa?", required:true, defaultValue:state.includeSpa
              input name:"includeChlorinator", type:"bool", title: "Show Chlorinator Section?", required:true, defaultValue:state.includeChlor
              input name:"includeIntellichem", type:"bool", title: "Show Intellichem Section?", required:true, defaultValue:state.includeChem
              input name:"includeSolar", type:"bool", title: "Enable Solar?", required:true, defaultValue:state.includeSolar
                }
            }
    	}
    else {
    	return dynamicPage(name: "poolConfig", title: "Getting Pool Controller Configuration...", nextPage: "poolConfig", refreshInterval: 4, install: false, uninstall: false) {
        getPoolConfig()
    	}
	}
}



def getPoolConfig() {
 	atomicState.config=false
    def devMAC = selectedDevice
    def devices = getVerifiedDevices()
    def selectedDeviceInfo = devices.find { it.value.mac == devMAC }
    if (selectedDeviceInfo) {
        def value = selectedDeviceInfo.value
        log.debug "Configure [${selectedDeviceInfo.value.mac}]"
        String port = getPort(value)
		String ip = getIP(value)
		String host = "${ip}:${port}"
        def params = [
            method: "GET",
            path: "/config",
            headers: [
                HOST: "${ip}:${port}",
                "Accept":"application/json"
            ]
        ]
        def opts = [
            callback : 'parseConfig',
            type: 'LAN_TYPE_CLIENT'
        ]
        try {
            sendHubCommand(new hubitat.device.HubAction(params, null, opts))
            log.debug "SENT: ${params}"
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
    else {
    	log.error("Failed to locate a verified controller for use with ${devMAC}")
        log.debug("Devices={$state.devices}")
        }
}

def parseConfig(resp) {
    def message = parseLanMessage(resp.description)
    def msg = message.json
	log.debug("parseConfig - msg=${msg.config}")
    log.debug("parseConfig-circuit - msg=${msg.circuit}")

    /* state.includeSolar = msg.config.equipment.solar.installed == 1
    state.includeSpa = msg.config.equipment.spa.installed == 1
    state.controller = msg.config.equipment.controller
    state.circuitHideAux = msg.config.equipment.circuit.hideAux
    state.numCircuits =  msg.config.equipment.circuit.nonLightCircuit.size() + msg.config.equipment.circuit.lightCircuit.size()
    state.nonLightCircuits = msg.config.equipment.circuit.nonLightCircuit
    state.lightCircuits = msg.config.equipment.circuit.lightCircuit */

    state.bodies = msg.bodies
    state.pumps = msg.pumps
    state.circuits = msg.circuits
    // state.includeChem = msg.intellichem.installed == 1
    // state.includeChlor = msg.chlorinators[0].isActive > 0
    state.equipment = msg.equipment
    state.pumps = msg.pumps
    state.features = msg.features
    state.valves = msg.valves
    state.config=true

    // Extra data not currently used
    state.controllerType = msg.controllerType
    state.pool = msg.pool
    state.configVersion = msg.configVersion
    state.schedules = msg.schedules
    state.lastUpdated = msg.lastUpdated

    log.info "STATE=${state}"
}


def USN() {
	return "urn:schemas-upnp-org:device:PoolController:1"
}

void ssdpDiscover() {
    def searchTarget = "lan discovery " + USN()
    sendHubCommand(new hubitat.device.HubAction("${searchTarget}", hubitat.device.Protocol.LAN))
}

void ssdpSubscribe() {
	 if (!atomicState.subscribed) {
        log.trace "Discover Devices: subscribe to location " + USN()
     	subscribe(location, null, ssdpHandler, [filterEvents: false])
        // subscribe(USN(), null, ssdpHandler, [filterEvents: false])
        atomicState.subscribed = true
     }
}

def ssdpHandler(evt) {
	def description = evt.description
	def hub = evt?.hubId
	def parsedEvent = parseLanMessage(description)
	parsedEvent << ["hub":hub]
    //log.debug("SDP Handler - ${parsedEvent}")
 	if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:PoolController:1")) {
		def devices = getDevices()
        String ssdpUSN = parsedEvent.ssdpUSN.toString()
        //log.debug("GET SSDP - found a pool ${ssdpUSN}")
        if (devices."${parsedEvent.mac}") {
            def d = devices."${parsedEvent.mac}"
            if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
                d.networkAddress = parsedEvent.networkAddress
                d.deviceAddress = parsedEvent.deviceAddress
                def child = getChildDevice(parsedEvent.mac)
                if (child) {
                    child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
                }
            }
        } else {
            //log.debug("Adding to Devices to be verified")
            parsedEvent.verified = false
            atomicState.devices[parsedEvent.mac] = parsedEvent
            state.devices[parsedEvent.mac] = parsedEvent
        }
    } else {
        // log.debug("Not matching parsed event received - ${parsedEvent}")
    }
}

def getVerifiedDevices() {
	getDevices().findAll{ key, value -> value.verified == true }
}

def getDevices() {
	if (!atomicState.devices) {
        log.debug("RESET AS DEVICES")
		atomicState.devices = [:]
	}
    if (!state.devices) {
        log.debug("RESET DEVICES")
		state.devices = [:]
	}
	return state.devices
}

void verifyDevices() {
	def devices = getDevices()
    //log.debug("VerifyDevices(pre) - ${devices}")
    devices = devices.findAll { key, value ->
        value.verified != true
    }
    //log.debug("VerifyDevices - ${devices}")
	devices.each { key, value ->
        String port = getPort(value)
		String ip = getIP(value)
		String host = "${ip}:${port}"
        log.info("Verify UPNP PoolController Device ${value.mac} @ http://${host}${value.ssdpPath}")
        //log.debug("SENDING HubAction: GET ${value.ssdpPath} HTTP/1.1  HOST: ${host}")
		sendHubCommand(new hubitat.device.HubAction("""GET ${value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", hubitat.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
	}
}

void deviceDescriptionHandler(hubitat.device.HubResponse hubResponse) {
	def body = hubResponse.xml
    log.debug("DevDescHandler - > ${body}")
    def devices = getDevices()
    log.debug("DevDescHandler -devs- > ${devices}")
	if (body) {
        log.debug("DDH -UDN--> ${body.device.UDN?.text()}")
        def device = devices.find {
            def cleanUDN = body.device.UDN.text().toLowerCase().replaceAll(':','').replaceAll('-','')
            def cleanKey = it?.key?.toLowerCase()
            log.debug(cleanUDN)
            log.debug(cleanKey)
            log.debug(cleanUDN.contains(cleanKey))
            return cleanUDN.contains(cleanKey)
        }
        log.info("VERYFY ${device.key} - ${body}")
        if (device) {
            device.value << [name: body?.device?.friendlyName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
            device.value.verified = true
            log.info("Verified a device - device.value == ${device}")
        }
        else {
            log.error("Cannot verify Device - Device Not found")
        }
    }
    else {
        log.error("Cannot verify Device - No body in Device Response")
    }
}


def addDevice() {
	def devices = getDevices()
	def selectedDeviceInfo = devices.find { it.value.mac == selectedDevice }
    if (selectedDeviceInfo) {
        createOrUpdateDevice(selectedDeviceInfo.value.mac,getIP(selectedDeviceInfo.value),getPort(selectedDeviceInfo.value))
    }
}

def addManualDevice() {
	if (controllerMac && controllerIP && controllerPort) {  createOrUpdateDevice(controllerMac,controllerIP,controllerPort)	}
}

def createOrUpdateDevice(mac,ip,port) {
	def hub = location.hubs[0]
	//log.error("WARNING Using TEST MAC")
    //mac = mac + "-test"
	def d = getChildDevice(mac)
    if (d) {
        log.info "The Pool Controller Device with dni: ${mac} already exists...update config to ${ip}:${port}"
        d.updateDataValue("controllerIP",ip)
        d.updateDataValue("controllerPort",port)
        d.updated()
   }
   else {
   		log.info "Creating Pool Controller Device with dni: ${mac}"
		d = addChildDevice("bsileo", "Pool Controller", mac, hub.id, [
			"label": deviceName,
            "completedSetup" : true,
			"data": [
				"controllerMac": mac,
				"controllerIP": ip,
				"controllerPort": port,
				]
			])
   }
}

def getPort(value) {
    String port
    if ( value.port ) {
        port = value.port
    }
    else {
	  port = convertHexToInt(value.deviceAddress)
    }
    return port
}

def getIP(value) {
    String ip
    if (value.ipaddress) {
        ip = value.ipaddress
    }
    else {
        ip = convertHexToIP(value.networkAddress)
    }
    return ip
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}