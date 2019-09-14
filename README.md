# What is the Hubitat Pool Controller?
A collection of devices designed to interface with a nodejs-poolController instance which is talking on the RS-485 bus to allow viewing and setting pool control options. Includes devices to manage the Pool pump, lights and heater, the spa pump and heater, the chlorinator, and any installed additional "Features".
# License
Copyright (C) 2017-2019  Brad Sileo / bsileo / brad@sileo.name
## Installation Instructions

1. Install and configure Nodejs-Poolcontroller (version 5.0+)
          https://github.com/tagyoureit/nodejs-poolController/tree/v5.0.0
2. Update your Nodejs-Poolcontroller installation with the SmartThings interface:
   
   Update your configuration file to enable "OutputToHubitat". Note that the "*" format for an address is NOT supportred with Hubitat.
	 ```"integrations": {
        	"outputToHubitat": 1
    		},
    	"outputToSmartHubitat": {
        	"address": "192.168.1.XXX",
        	"port": "39501",
        	"logEnabled": 1
    	},```
3. Install the new Apps and Drivers into Hubitat
4. Install the new App
5. Use the newly created devices in Dashboards, Rules, Groups, etc!!
