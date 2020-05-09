# What is the Hubitat Pool Controller?
A collection of devices designed to interface with a nodejs-poolController instance which is talking on the RS-485 bus to allow viewing and setting pool control options. Includes devices to manage the Pool pump, lights and heater, the spa pump and heater, the chlorinator, all Circuits and Features, and Intellichem devices.

# License
Copyright (C) 2017-2020  Brad Sileo / bsileo / brad@sileo.name

## Note
This version is NOT compatible with the 5.3.3 version of nodejs-poolController. If you are using that version, consider upgrading! The last deprecated version of this code for use with 5.3.3 is available [here](https://github.com/bsileo/hubitat_poolcontroller/tree/NJPC-5.3.3). There is no forward migration path from that version to this version as all Apps and Drivers have been renamed and refactored. 

## Installation Instructions

1. Install and configure [Nodejs-Poolcontroller](https://github.com/tagyoureit/nodejs-poolController) (version [NEXT](https://github.com/tagyoureit/nodejs-poolController/tree/next) is required!)
          https://github.com/tagyoureit/nodejs-poolController
2. Update your [Nodejs-Poolcontroller-webclient](https://github.com/tagyoureit/nodejs-poolController-webClient) installation with the Hubitat interface:  (details pending)
   ```
   	"address": "192.168.1.XXX",
        "port": "39501"        
   ```
3. Open the Apps Code, "New App" and then either:

- Click Import, then paste in the URL to the file: https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_app.groovy

- Or paste the code for the Master App into it and Save:

	* [pool_controller_app.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_app.groovy) 
     	
4. Install all of the Drivers into Drivers Code following this same procedure:
     	
	* [pool_controller.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller.groovy)
	* [pool_controller_body.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_body.groovy)
	* [pool_controller_chlorinator.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6//pool_controller_chlorinator.groovy)
	* [pool_controller_heater.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_heater.groovy)
	* [pool_controller_intellibrite.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_intellibrite.groovy)
	* [pool_controller_intellichem.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/NJSPC6/pool_controller_intellichem.groovy)
	* [pool_controller_pump.groovy](https://github.com/bsileo/hubitat_poolcontroller/blob/master/pool_controller_pump.groovy)


5. Go to Apps, Add User App and create a "Pool Controller 6" app. The Nodejs-Poolcontroller should be autolocated, or you can manually enter the details. Follow the prompts to complete installation.
6. Use the newly created devices in Dashboards, Rules, Groups, etc!!
