const app = express();
const express = require("express");
const fs = require("fs");
const shutdownServer = require("http-graceful-shutdown");

app.use(express.json({limit: "50mb"}));

// This file will be glued to the top of the specific xy-serve.js file

app.post('/shutdown', (req, res) => {
    res.status(200).send('Shutting down');
    setTimeout(async () => {
        try {
            await shutdownServer(
                app.listen(0, '127.0.0.1', () => {
                    const instanceId = getInstanceId();
                    debugLog('Server running on port ' + listener.address().port + ' for instance ' + instanceId);
                    fs.writeFile('server.port.tmp', '' + listener.address().port, function (err) {
                        if (err) {
                            return console.log(err);
                        } else {
                            // Try to be as atomic as possible
                            fs.rename('server.port.tmp', `server-${instanceId}.port`, function (err) {
                                if (err) {
                                    return console.log(err);
                                }
                            });
                        }
                    });
                }),
                {
                    forceExit: false, // Let the event loop clear
                    finally: () => debugLog('graceful shutdown finished.'),
                }
            )();
        } catch (err) {
            console.error('Error during shutdown:', err);
        }
    }, 200);
});

function getInstanceId() {
    return (
        process.argv
            .slice(2)
            .find(arg => arg.startsWith('--node-server-instance-id='))
            ?.split('=')[1]
            || (() => {
                throw new Error('Missing --node-server-instance-id argument');
            })()
    );
}

function debugLog() {
    if (false) { // Set to true for debug log output in node process
        console.log.apply(this, arguments);
    }
}
