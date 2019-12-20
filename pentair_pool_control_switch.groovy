/**
 *  Copyright 2019 Brad Sileo
 *
 *  Author: Brad Sileo
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
	definition (name: "Pentair Pool Control Switch", 
            namespace: "bsileo", 
            author: "Brad Sileo",
            importUrl: 'https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/pentair_pool_control_switch.groovy') {
		capability "Switch"
        command "onConfirmed"
        command "offConfirmed"
        attribute "friendlyName", "string"
        attribute "circuitFunction", "string"
	}
}

def installed() {	
}

def parse(String description) {
	try {
         def pair = description.split(":")
         createEvent(name: pair[0].trim(), value: pair[1].trim())
     }
     catch (java.lang.ArrayIndexOutOfBoundsException e) {
           log.debug "Error! " + e   
    }
	
}

def onConfirmed() {
    //log.debug("CONF ${device} turned on")
	sendEvent(name: "switch", value: "on", displayed:true)    
}

def offConfirmed() {
	//log.debug("CONF ${device} turned off")
	sendEvent(name: "switch", value: "off", displayed:true)  
}

def on() {
	parent.childOn(device.deviceNetworkId)
    sendEvent(name: "switch", value: "turningOn", displayed:false,isStateChange:false)    
}

def off() {
	parent.childOff(device.deviceNetworkId)
    sendEvent(name: "switch", value: "turningOff", displayed:false,isStateChange:false)
}

def setFriendlyName(name) {
   //log.debug("Set FName to ${name}")
   sendEvent(name: "friendlyName", value: name, displayed:false)
}

def setCircuitFunction(name) {
   //log.debug("Set CircuitFunction to ${name}")
   sendEvent(name: "circuitFunction", value: name, displayed:false)
}
