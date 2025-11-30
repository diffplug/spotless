import express from 'express';
import fs from 'fs';
import shutdownServer from 'http-graceful-shutdown';

// this file will be glued to the top of the specific xy-serve.js file

init(express());

function init(app) {
    app.use(express.json({ limit: "50mb" }));
    app.post("/shutdown", (req, res) => {
        res.status(200).send("Graceful shutdown.");
        setTimeout(async () => {
            try {
                await shutdownServer(app.listen(0, "127.0.0.1", () => streamOutput(server, getInstanceId())), {
                    forceExit: false, // let the event loop clear
                    finally: () => debugLog("graceful shutdown finished."),
                })();
            } catch (err) {
                console.error("Error during shutdown:", err);
            }
        }, 200);
    });
}

function getInstanceId() {
    return process.argv.slice(2).find(arg => arg.startsWith("--node-server-instance-id="))?.split("=")[1]
        || (() => { throw new Error("Missing --node-server-instance-id argument"); })();
}

function streamOutput(server, instanceId) {
    debugLog("Server running on port " + server.address().port + " for instance " + instanceId);
    fs.writeFile("server.port.tmp", "" + server.address().port, function (err) {
        if (err) {
            return console.log(err);
        } else {
            fs.rename("server.port.tmp", `server-${instanceId}.port`, function (err) {
                if (err) {
                    return console.log(err);
                }
            });
        }
    });
}

function debugLog() {
    if (false) { // set to true for debug log output in node process
        console.log.apply(this, arguments);
    }
}
