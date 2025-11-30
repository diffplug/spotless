const express = require("express");
const fs = require("fs");
const shutdownServer = require("http-graceful-shutdown");

// this file will be glued to the top of the specific xy-serve.js file

function getInstanceId() {
    return process.argv.slice(2).find(arg => arg.startsWith("--node-server-instance-id="))?.split("=")[1]
        || (() => { throw new Error("Missing --node-server-instance-id argument"); })();
}

function streamOutput(port, instanceId) {
    debugLog("Server running on port " + port + " for instance " + instanceId);
    fs.writeFile("server.port.tmp", "" + port, function (err) {
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

const app = express();

app.use(express.json({ limit: "50mb" }));

const server = app.listen(0, "127.0.0.1", () => streamOutput(server.address().port, getInstanceId()));

app.post("/shutdown", (req, res) => {
    setTimeout(async () => {
        try {
            await shutdownServer(server, {
                forceExit: false, // let the event loop clear
                finally: () => debugLog("graceful shutdown finished."),
            })();
        } catch (err) {
            console.error("Error during shutdown:", err);
        }
    }, 200);
    res.ok(200).send("Graceful shutdown.");
});
