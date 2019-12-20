# What is the Hubitat Pool Controller?
A collection of devices designed to interface with a nodejs-poolController instance which is talking on the RS-485 bus to allow viewing and setting pool control options. Includes devices to manage the Pool pump, lights and heater, the spa pump and heater, the chlorinator, and any installed additional "Features".
# License
Copyright (C) 2017-2019  Brad Sileo / bsileo / brad@sileo.name
## Installation Instructions

1. Install and configure Nodejs-Poolcontroller (version 5.3.x is currently supported)
          https://github.com/tagyoureit/nodejs-poolController/tree/v5.0.0
2. Update your Nodejs-Poolcontroller installation with the Hubitat interface:
   - Add the file outputToHubittat.js into the "integrations" directory on nodejsPoolController
   - Update your configuration file to enable "OutputToHubitat". Note that the "*" format for an address is NOT supported with Hubitat.
	 ```"integrations": {
        	"outputToHubitat": 1
    		},
    	"outputToHubitat": {
        	"address": "192.168.1.XXX",
        	"port": "39501",
        	"logEnabled": 1
    	},```
3. Open the Apps Code, "new App" and then paste the code for the <Master App>[https://github.com/bsileo/hubitat_poolcontroller/blob/master/poolControllerApp.groovy] into it and Save.
4. Install all of the Drivers into Drivers Code.
5. Go to Apps, Add User App and create a "Pool Controller" app. The Nodejs-Poolcontroller shoudl be autolocated, or you can manually enter the details
6. Use the newly created devices in Dashboards, Rules, Groups, etc!!
