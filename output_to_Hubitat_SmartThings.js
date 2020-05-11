var http = require('http');
var io = require('socket.io-client');
var patch = require('socketio-wildcard')(io.Manager);
// var configFile = container.settings.getConfig()

var servers = [
    {address: '192.168.2.106', port: 39501},
    {address: '192.168.2.128', port: 39500}
]

var serverURL;
serverURL = 'http://localhost:4200'


var socket = io.connect(serverURL, {
    secure: false,
    // secure: secureTransport,
    reconnect: true,
    rejectUnauthorized: false
});
patch(socket);
function notify(event, data, address, port) {
    if (address !== '*') {
        var json = JSON.stringify(data);
        var opts = {
            method: 'NOTIFY',
            host: address,
            port: port,
            path: '/notify',
            headers: {
                'CONTENT-TYPE': 'application/json',
                'CONTENT-LENGTH': Buffer.byteLength(json),
                'X-EVENT-TYPE': event
            }
        };
        //console.log(event)
        var req = http.request(opts);
        req.on('error', function (err, req, res) {
           console.error(err);
        });
        req.write(json);
        req.end();
        console.log('outputToHub (' + address + ':' + port + ') Sent ' + event + " '" + json + "'");
    }
}

socket.on('*', function (data) {
    servers.forEach( (server) => {
        notify(data.data[0], data.data[1], server.address, server.port);
    })
});
