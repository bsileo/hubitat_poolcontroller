/**
 *  Copyright 2019 Brad Sileo
 *
 *
 *  Intellibrite Color Mode Device
 *
 *  Author: Brad Sileo
 *
 *  Date: 2018-10-21
 */
metadata {
	definition (name: "Pentair Intellibrite Color Light Mode", namespace: "bsileo", author: "Brad Sileo") {
		capability "Momentary"
	}
}


def installed() {
	log.debug("Installed Intellibrite Color Mode color=" + device.deviceNetworkId)
}

def updated() {
}


def parse(String description) {
}

def push() {
    def mode = getDataValue("modeName")
    def circuitID = getDataValue("circuitID")
    parent.setColor(circuitID, getColorOrModeID())	
}

def getColorOrModeID() {
	def colorID 
    def colorIDLookup = ["White" : 0,
        "Custom" :1,
        "Light Green":2,
        "Green":4,
        "Cyan":6,
        "Blue":8,
        "Lavender":10,
        "Magenta":12,
        "Light Magenta":14,
        'Off': 0,
        'On': 1,
        'Color Sync': 128,
        'Color Swim': 144,
        'Color Set': 160,
        'Party': 177,
        'Romance': 178,
        'Caribbean': 179,
        'American': 180,
        'Sunset': 181,
        'Royal': 182,
        'Save': 190,
        'Recall': 191,
        'Blue': 193,
        'Green': 194,
        'Red': 195,
        'White': 196,
        'Magenta': 197
        ]
    def mode = getDataValue("modeName")
    return colorIDLookup[mode]    
}
