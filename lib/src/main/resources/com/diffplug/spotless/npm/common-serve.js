const shutdownServer = require("http-graceful-shutdown");
const express = require("express");
const app = express();
const fs = require("fs");

app.use(express.json({ limit: "50mb" }));

// this file will be glued to the top of the specific xy-serve.js file

let listener = app.listen(0, "127.0.0.1", () => {
	const instanceId = getInstanceId();
	debugLog("Server running on port " + listener.address().port + " for instance " + instanceId);
	fs.writeFile("server.port.tmp", "" + listener.address().port, function (err) {
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
});

// todo: this seems untested?
//app.post("/shutdown", (req, res) => {
//	res.status(200).send("Shutting down");
//	setTimeout(async () => {
//		try {
//			await shutdownServer(listener, {
//                  	forceExit: false, // let the event loop clear
//                  	finally: () => debugLog("graceful shutdown finished."),
//                  })();
//		} catch (err) {
//			console.error("Error during shutdown:", err);
//		}
//	}, 200);
//});

function getInstanceId() {
    return process.argv.slice(2).find(arg => arg.startsWith("--node-server-instance-id="))?.split("=")[1] ||
        (() => { throw new Error("Missing --node-server-instance-id argument"); })();
}

function debugLog() {
    if (false) { // set to true for debug log output in node process
        console.log.apply(this, arguments);
    }
}
