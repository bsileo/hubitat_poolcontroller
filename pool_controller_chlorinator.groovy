/**
 *  Copyright 2020 Brad Sileo
 *
 *  Pool Controller Chlorinator
 *
 *  Author: Brad Sileo
 *
 */
metadata {
	definition (name: "Pool Controller Chlorinator",
            namespace: "bsileo",
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pool_controller_chlorinator.groovy') {
		capability "Refresh"
        capability "Switch"
		attribute "saltLevel", "string"
        attribute "targetOutput", "string"
		attribute "currentOutput", "string"
        attribute "status", "string"
        attribute "saltRequired", "string"
        
		attribute "superChlorHours", "number"		
		attribute "poolSetpoint", "string"
        attribute "spaSetpoint", "string"
        
        command "setPoolSetpoint", [[name:"Pool Setpoint*",
                                      "type":"ENUM",
                                      "description":"Set the output level for the Pool",
                                      "constraints":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,
                                                     21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,
                                                     40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,
                                                     60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,
                                                     80,81,82,82,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,
                                                     100]
                                     ]]
        command "setSpaSetpoint", [[name:"Spa Setpoint*",
                                      "type":"ENUM",
                                      "description":"Set the output level for the Spa",
                                      "constraints":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,
                                                     21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,
                                                     40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,
                                                     60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,
                                                     80,81,82,82,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,
                                                     100]
                                     ]]         
         
        command "setSuperChlorHours", [[name:"Super Chlor Status*",
                                        "type":"ENUM",
                                        "constraints":["On","Off"]
                                        ],
                                        [name:"Super Chlor Hours*",
                                      "type":"ENUM",
                                      "description":"Set the output level for the Spa",
                                      "constraints":[0,1,2,3,4,5,6,7,8]
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
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}

def installed() {
}

def updated() {
  state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 5
}


def initialize() {

}

def parse(msg) {
     sendEvent([name: "currentOutput", value: msg.currentOutput])
     sendEvent([name: "targetOutput", value: msg.targetOutput])
     sendEvent([name: "saltLevel", value: msg.saltLevel])
     sendEvent([name: "saltRequired", value: msg.saltRequired ? 'Yes' : 'No'])
     sendEvent([name: "poolSetpoint", value: msg.poolSetpoint])
     sendEvent([name: "spaSetpoint", value: msg.spaSetpoint])
    sendEvent([name: "superChlorHours", value: msg.superChlorHours])
     if (msg.status) {
         sendEvent([name: "status", value: msg.status.name, descriptionText: "Chlorinator status is ${msg.status.desc}"])
     }    
}

// Command Implementations
def refresh() {
    logger("Requested a refresh","info")
    def id = getDataValue("id")
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/${id}"
    ]
    logger("Refresh with ${params} - ${data}","debug")
    asynchttpGet('parseRefresh', params, data)
}

def parseRefresh (response, data) {
    logger("parseRefresh - ${response.json}","debug")
    try {
        def value = response.getJson()
        parse(value)
    }
    catch (e) {
        logger("Failed to refresh Chlorinator due to ${e}","error")
    }
}


def on() {
    return chlorinatorOn()
}

def off() {
   return chlorinatorOff()
}

def chlorinatorOn() {
   return chlorinatorUpdate(70,30,0)
}

def chlorinatorOff() {
   return chlorinatorUpdate(0,0,0)
}

def setPoolSetpoint(poolLevel) {    
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/poolSetPoint",
        requestContentType: "application/json",
        contentType: "application/json",
        body: [id: getDataValue("id"), setPoint: poolLevel ]
    ]
    data = [device: device, item: 'poolSetpoint', value: poolLevel]
    
    logger("Update Chlorinator with poolSetpoint to ${poolLevel}","info")
    logger("Update Chlorinator with PUT ${params} - ${data}","debug")    
    asynchttpPut('updateCallback', params, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def setSpaSetpoint(spaLevel) {
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/spaSetPoint",
        requestContentType: "application/json",
        contentType: "application/json",
        body: [id: getDataValue("id"), setPoint: spaLevel ]        
    ]
    data = [device: device, item: 'spaSetpoint', value: spaLevel]
    
    logger("Update Chlorinator with spaSetpoint to ${spaLevel}","info")
    logger("Update Chlorinator with PUT ${params} - ${data}","debug")    
    asynchttpPut('updateCallback', params, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def setSuperChlorHours(status, hours) {
    logger("Super chlor  ${hours} ${status}","trace")
     def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/superChlorHours",
        requestContentType: "application/json",
        contentType: "application/json",
        body: [id: getDataValue("id"), hours: hours, superChlorinate : status == 'On' ? 1 : 0 ]        
    ]
    data = [device: device, item: 'superChlor', value: hours]
    
    logger("Update Chlorinator with SuperChlor to ${hours}","info")
    logger("Update Chlorinator with PUT ${params} - ${data}","debug")    
    asynchttpPut('updateCallback', params, data)
    
    def params2 = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/superChlorinate",
        requestContentType: "application/json",
        contentType: "application/json",
        body: [id: getDataValue("id"), hours: hours, superChlorinate : status == 'On' ? 1 : 0 ]        
    ]
    
    logger("Update Chlorinator with SuperChlorinate to ${status}","info")
    asynchttpPut('updateCallback', params2, data)
    
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}


def chlorinatorUpdate(poolLevel = null, spaLevel = null, superChlorHours = null) {
    def id = getDataValue("id")
    def body = [
        id: id,
        poolSetPoint: poolLevel ? poolLevel : device.currentValue("poolSetpoint"),
        spaSetPoint: spaLevel ? spaLevel : device.currentValue("spaSetpoint") ,
        superChlorHours: superChlorHours ? superChlorHours : device.currentValue("superChlorHours")
    ]
    def params = [
        uri: getParent().getControllerURI(),
        path: "/state/chlorinator/setChlor)",
        requestContentType: "application/json",
        contentType: "application/json",
        body: body
    ]
    logger("Update Chlorinator with ${body}","info")
    logger("Update Chlorinator with PUT ${params} - ${body}","debug")
    asynchttpPut('updateCallback', params, data)
    sendEvent(name: "switch", value: "on", displayed:false,isStateChange:false)
}

def updateCallback(response, data) {
    if (response.getStatus() == 200) {        
        logger("State Change Result ${response.getStatus()}","debug")
        logger("State change complete","info")
    } else {
        logger("State change failed - ${response.getStatus()}","error")
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
