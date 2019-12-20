module.exports = function (container) {

    var http = container.http

    var configFile = container.settings.getConfig()

    var address = configFile.outputToHubitat.address
    var port = configFile.outputToHubitat.port
    var secureTransport = configFile.poolController.https.enabled === 1 ? true : false
    var logEnabled = 0


    var serverURL;

    if (configFile.outputToHubitat.hasOwnProperty("logEnabled")) {
        logEnabled = configFile.outputToHubitat.logEnabled
    }


    if (secureTransport) {
        serverURL = 'https://localhost:' + configFile.poolController.https.expressPort
    } else {
        serverURL = 'http://localhost:' + configFile.poolController.http.expressPort
    }

    var io = container.socketClient
    var socket = io.connect(serverURL, {
        secure: secureTransport,
        reconnect: true,
        rejectUnauthorized: false
    });

    function notify(event, data) {
        if (address !== '*') {
            var json = JSON.stringify(data)

            var opts = {
                method: 'NOTIFY',
                host: address,
                port: port,
                path: '/notify',
                headers: {
                    'CONTENT-TYPE': 'application/json',
                    'CONTENT-LENGTH': Buffer.byteLength(json),
                    'X-EVENT': event,
                }
            };

            var req = http.request(opts);
            req.on('error', function (err, req, res) {
                container.logger.error(err);
            });
            req.write(json);
            req.end();
            if (logEnabled) {
                container.logger.debug('outputToHubitat sent event %s', event)
                container.logger.silly('outputToHubitat (' + address + ':' + port + ') Sent ' + event + "'" + json + "'")
            }
        }

    }

    socket.on('all', function (data) {
        notify('all', data)
    })

    socket.on('circuit', function (data) {
        notify('circuit', data)
    })


    socket.on('chlorinator', function (data) {
        notify('chlorinator', data)
    })

    function init() {

            container.logger.info('outputToHubitat Loaded. \n\taddress: %s\n\tport: %s\n\tsecure: %s', address, port, secureTransport)
        if (!logEnabled) {
            container.logger.info('outputToHubitat log NOT enabled.  This will be the last message displayed by this integration.')

        }
    }

    var mdns = container.server.mdnsEmitter;
    // Note - MDNS not avaialble in Hubitat at this time.
    /*mdns.on('response', function (response) {
        if (address !== response.additionals[2].data) {
            //console.log('in ST: TXT data:', response.additionals[0].data.toString())
            //console.log('in ST: SRV data:', JSON.stringify(response.additionals[1].data))
            //console.log('in ST: IP Address:', response.additionals[2].data)
            address = response.additionals[2].data
            if (logEnabled) {
                container.logger.info('outputToHubitat: Hub address updated to:', address)
            }
            // close mdns server if we don't need it... or keep it open if you think the IP address of ST will change
            container.server.closeAsync('mdns')
        }

    })
    container.server.mdnsQuery('_hubitat._tcp.local')
    */
    return {
        init: init
    }
}
