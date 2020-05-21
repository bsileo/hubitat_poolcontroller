# What is the Hubitat/SmartThings Pool Controller?
A collection of devices designed to interface with a nodejs-poolController instance which is talking on the RS-485 bus to allow viewing and setting pool control options. Includes devices to manage the Pool pump, lights and heater, the spa pump and heater, the chlorinator, all Circuits and Features, and Intellichem devices.

This code is fully compatible with BOTH SmartThings (classic App) and Hubitat.  The [SHPL](https://github.com/SANdood/SmartThings-Hubitat-Portability-Library) is awesome and made that possible so thanks to [Barry Burke](https://github.com/SANdood)

# License
Copyright (C) 2017-2020  Brad Sileo / bsileo / brad@sileo.name

## Note
This version is *NOT* compatible with the 5.3.3 version of nodejs-poolController. If you are using that version, consider upgrading! The last deprecated version of this code for use with 5.3.3 is available [here](https://github.com/bsileo/hubitat_poolcontroller/tree/NJPC-5.3.3). There is no forward migration path from that version to this version as all Apps and Drivers have been renamed and refactored.

## Installation Instructions
1. Install and configure [Nodejs-Poolcontroller](https://github.com/tagyoureit/nodejs-poolController) (version [NEXT](https://github.com/tagyoureit/nodejs-poolController/tree/next) is required!)
          https://github.com/tagyoureit/nodejs-poolController

## Hubitat - Package Manager

2. Install and configure the [Hubitat Package Manager](https://github.com/dcmeglio/hubitat-packagemanager) and use it to install this code

## SmartThings - GitHub Integration
2. Add the GitHub Repository to your  My SmartApps and My Device Handlers. Do an "Update from Repo" for [My SmartApps](https://graph.api.smartthings.com/ide/apps) and [My Device Handlers](https://graph.api.smartthings.com/ide/devices) and Publish to install
- Namespace: bsileo
- Name: hubitat_poolcontroler
- Branch: master

## Manual Approach on either Hub
3. Open Apps Code / My SmartApps, select "New App" / "New SmartApp" and then either:

- Paste the code for the Master App into it and Save:

	* [pool_controller_app.groovy](https://raw.githubusercontent.com/bsileo/hubitat_poolcontroller/master/smartapps/bsileo/pool-controller.src/pool-controller.groovy)

3. Install all of the [Drivers](https://github.com/bsileo/hubitat_poolcontroller/tree/master/devicetypes/bsileo) into Drivers Code following this same procedure:

	* pool_controller.groovy
	* pool_controller_body.groovy
	* pool_controller_chlorinator.groovy
	* pool_controller_heater.groovy
	* pool_controller_intellibrite.groovy
	* pool_controller_intellichem.groovy
	* pool_controller_pump.groovy


4. Go to Apps, Add User App and create a "Pool Controller" app. The Nodejs-Poolcontroller should be autolocated, or you can manually enter the details. Follow the prompts to complete installation.

5. Enable the Event interface in the poolController. Look for a section like the following and make the appropaite changes to set the Host, Post (39500 for SmartTHings, 39501 for Hubitat, and Enable to true):

```
 "smartThings": {
        "name": "SmartThings",
        "enabled": true,
        "fileName": "smartThings.json",
        "globals": {},
        "options": {
          "host": "10.0.0.39",
          "port": 39501
        }
```

6. Use the newly created devices in Dashboards, Rules, Groups, etc!!
